const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const User = require('../models/User');
const {
    validateEmail,
    validatePhoneNumber,
    normalizePhoneNumber,
    validatePassword,
    validateFullName,
    sanitizeInput
} = require('../utils/validators');
const { rateLimiter, sessionManager } = require('../utils/security');

// Account lockout configuration
const MAX_FAILED_ATTEMPTS = 5; // Maximum failed login attempts
// Khóa vĩnh viễn: set lockedUntil thành 100 năm sau (coi như vĩnh viễn)
const PERMANENT_LOCK_DATE = new Date(Date.now() + 100 * 365 * 24 * 60 * 60 * 1000); // 100 năm sau

// POST /api/auth/register
router.post('/register', async (req, res) => {
    try {
        const { email, phone, password, fullName } = req.body;
        console.log(`[REGISTER] Received request: email="${email}", phone="${phone}", fullName="${fullName}"`);

        // Check if all fields are provided
        if (!email || !phone || !password || !fullName) {
            return res.status(400).json({
                success: false,
                error: 'Vui lòng điền đầy đủ thông tin'
            });
        }

        // Validate full name
        if (!validateFullName(fullName)) {
            return res.status(400).json({
                success: false,
                error: 'Họ và tên không hợp lệ (tối thiểu 2 ký tự, tối đa 50 ký tự)'
            });
        }

        // Validate email
        const isEmailValid = validateEmail(email);
        console.log(`[REGISTER] Email validation for "${email}": ${isEmailValid}`);
        if (!isEmailValid) {
            return res.status(400).json({
                success: false,
                error: 'Email không hợp lệ'
            });
        }

        // Validate phone number
        if (!validatePhoneNumber(phone)) {
            return res.status(400).json({
                success: false,
                error: 'Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam (10-11 số, bắt đầu bằng 0)'
            });
        }

        // Validate password
        const passwordValidation = validatePassword(password);
        if (!passwordValidation.isValid) {
            return res.status(400).json({
                success: false,
                error: passwordValidation.message
            });
        }

        // Normalize and sanitize inputs
        const normalizedPhone = normalizePhoneNumber(phone);
        const sanitizedFullName = sanitizeInput(fullName);
        const normalizedEmail = email.toLowerCase().trim();

        // Check if user exists (using normalized values)
        const existingUser = await User.findOne({
            $or: [
                { email: normalizedEmail },
                { phone: normalizedPhone }
            ]
        });
        if (existingUser) {
            return res.status(400).json({
                success: false,
                error: 'Email hoặc số điện thoại đã tồn tại'
            });
        }

        // Hash password
        const hashedPassword = await bcrypt.hash(password, 10);

        // Create user
        const user = new User({
            email: normalizedEmail,
            phone: normalizedPhone,
            password: hashedPassword,
            fullName: sanitizedFullName
        });

        await user.save();

        // Return user without password
        const userResponse = user.toObject();
        delete userResponse.password;

        res.status(201).json({
            success: true,
            user: {
                id: user._id.toString(),
                email: user.email,
                phone: user.phone,
                fullName: user.fullName,
                createdAt: user.createdAt.getTime()
            }
        });
    } catch (error) {
        console.error('Register error:', error);

        // Handle duplicate key error (MongoDB unique constraint)
        if (error.code === 11000) {
            const field = Object.keys(error.keyPattern)[0];
            return res.status(400).json({
                success: false,
                error: `${field === 'email' ? 'Email' : 'Số điện thoại'} đã tồn tại`
            });
        }

        res.status(500).json({
            success: false,
            error: 'Server error'
        });
    }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            return res.status(400).json({
                success: false,
                error: 'Email và mật khẩu là bắt buộc'
            });
        }

        const normalizedEmail = email.toLowerCase().trim();
        const user = await User.findOne({ email: normalizedEmail });

        if (!user) {
            // Don't reveal if user exists or not (security best practice)
            return res.status(401).json({
                success: false,
                error: 'Email hoặc mật khẩu không đúng',
                remainingAttempts: MAX_FAILED_ATTEMPTS - 1
            });
        }

        // Check if account is locked
        const now = Date.now();
        if (user.lockedUntil && new Date(user.lockedUntil).getTime() > now) {
            const lockedUntilTime = new Date(user.lockedUntil).getTime();
            // Kiểm tra xem có phải khóa vĩnh viễn không (lockedUntil > 100 năm)
            const isPermanent = lockedUntilTime > Date.now() + 50 * 365 * 24 * 60 * 60 * 1000; // > 50 năm = vĩnh viễn

            // Get rate limit info (without incrementing)
            const ip = req.ip || req.connection.remoteAddress;
            const rateLimitKey = `auth:${ip}`;
            const rateLimitInfo = rateLimiter.getInfo(rateLimitKey, 5, 15 * 60 * 1000);

            if (isPermanent) {
                return res.status(423).json({
                    success: false,
                    error: 'Tài khoản đã bị khóa vĩnh viễn do quá nhiều lần đăng nhập sai',
                    locked: true,
                    permanent: true,
                    failedAttempts: user.failedLoginAttempts || MAX_FAILED_ATTEMPTS,
                    maxAttempts: MAX_FAILED_ATTEMPTS,
                    lockedUntil: user.lockedUntil,
                    message: 'Tài khoản đã bị khóa vĩnh viễn. Vui lòng liên hệ admin để mở khóa.',
                    rateLimit: {
                        remaining: rateLimitInfo.remaining,
                        reset: new Date(rateLimitInfo.resetTime).toISOString()
                    }
                });
            } else {
                const secondsRemaining = Math.ceil((lockedUntilTime - now) / 1000);
                const minutesRemaining = Math.ceil(secondsRemaining / 60);

                return res.status(423).json({
                    success: false,
                    error: 'Tài khoản đã bị khóa do quá nhiều lần đăng nhập sai',
                    locked: true,
                    permanent: false,
                    failedAttempts: user.failedLoginAttempts || MAX_FAILED_ATTEMPTS,
                    maxAttempts: MAX_FAILED_ATTEMPTS,
                    lockedUntil: user.lockedUntil,
                    secondsRemaining: secondsRemaining,
                    minutesRemaining: minutesRemaining,
                    message: `Tài khoản đã bị khóa. Vui lòng thử lại sau ${minutesRemaining} phút.`,
                    rateLimit: {
                        remaining: rateLimitInfo.remaining,
                        reset: new Date(rateLimitInfo.resetTime).toISOString()
                    }
                });
            }
        }

        // If lockout period has passed (và không phải khóa vĩnh viễn), reset failed attempts
        if (user.lockedUntil && new Date(user.lockedUntil).getTime() <= now) {
            user.failedLoginAttempts = 0;
            user.lockedUntil = null;
            await user.save();
        }

        // Verify password
        const isValid = await bcrypt.compare(password, user.password);

        if (!isValid) {
            // Increment failed login attempts
            user.failedLoginAttempts = (user.failedLoginAttempts || 0) + 1;
            user.lastLoginAttempt = new Date();
            // Lưu IP để có thể reset rate limiter khi unlock
            user.lastLoginIP = req.ip || req.connection.remoteAddress;

            // Lock account permanently if max attempts reached
            if (user.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                // Khóa vĩnh viễn: set lockedUntil thành 100 năm sau
                user.lockedUntil = PERMANENT_LOCK_DATE;
                await user.save();

                // Get rate limit info (without incrementing)
                const ip = req.ip || req.connection.remoteAddress;
                const rateLimitKey = `auth:${ip}`;
                const rateLimitInfo = rateLimiter.getInfo(rateLimitKey, 5, 15 * 60 * 1000);

                return res.status(423).json({
                    success: false,
                    error: 'Tài khoản đã bị khóa vĩnh viễn do quá nhiều lần đăng nhập sai',
                    locked: true,
                    permanent: true,
                    failedAttempts: user.failedLoginAttempts,
                    maxAttempts: MAX_FAILED_ATTEMPTS,
                    lockedUntil: user.lockedUntil,
                    message: `Đã vượt quá ${MAX_FAILED_ATTEMPTS} lần đăng nhập sai. Tài khoản đã bị khóa vĩnh viễn. Vui lòng liên hệ admin để mở khóa.`,
                    rateLimit: {
                        remaining: rateLimitInfo.remaining,
                        reset: new Date(rateLimitInfo.resetTime).toISOString()
                    }
                });
            }

            await user.save();

            const remainingAttempts = MAX_FAILED_ATTEMPTS - user.failedLoginAttempts;

            // Get rate limit info (without incrementing)
            const ip = req.ip || req.connection.remoteAddress;
            const rateLimitKey = `auth:${ip}`;
            const rateLimitInfo = rateLimiter.getInfo(rateLimitKey, 5, 15 * 60 * 1000);

            return res.status(401).json({
                success: false,
                error: 'Invalid email or password',
                failedAttempts: user.failedLoginAttempts,
                remainingAttempts: remainingAttempts,
                maxAttempts: MAX_FAILED_ATTEMPTS,
                message: `Sai mật khẩu. Còn ${remainingAttempts} lần thử.`,
                rateLimit: {
                    remaining: rateLimitInfo.remaining,
                    reset: new Date(rateLimitInfo.resetTime).toISOString()
                }
            });
        }

        // Login successful - reset failed attempts and unlock account
        user.failedLoginAttempts = 0;
        user.lockedUntil = null;
        user.lastLoginAttempt = new Date();
        await user.save();

        // Reset rate limiter cho IP này (vì đăng nhập thành công)
        const ip = req.ip || req.connection.remoteAddress;
        const rateLimitKey = `auth:${ip}`;
        rateLimiter.reset(rateLimitKey);


        // Create session
        const clientIP = req.ip || req.connection.remoteAddress || req.headers['x-forwarded-for']?.split(',')[0]?.trim();
        const userAgent = req.headers['user-agent'] || null;
        const sessionId = sessionManager.createSession(user._id.toString(), {
            ip: clientIP,
            userAgent: userAgent
        });

        // Get session info including expiresAt
        const sessionInfo = sessionManager.getSession(sessionId);

        // Calculate time until expiration
        const expiresAt = sessionInfo.expiresAt;
        const expiresIn = Math.floor((expiresAt - Date.now()) / 1000); // seconds until expiration
        const expiresInHours = Math.floor(expiresIn / 3600); // hours until expiration

        // Return user without password
        const userResponse = user.toObject();
        delete userResponse.password;

        res.json({
            success: true,
            user: {
                id: user._id.toString(),
                email: user.email,
                phone: user.phone,
                fullName: user.fullName,
                createdAt: user.createdAt.getTime()
            },
            sessionId: sessionId,
            session: {
                createdAt: sessionInfo.createdAt,
                createdAtISO: new Date(sessionInfo.createdAt).toISOString(),
                expiresAt: expiresAt,
                expiresAtISO: new Date(expiresAt).toISOString(),
                expiresIn: expiresIn, // seconds until expiration
                expiresInHours: expiresInHours // hours until expiration
            },
            message: 'Đăng nhập thành công'
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({
            success: false,
            error: 'Server error'
        });
    }
});

// POST /api/auth/logout
router.post('/logout', async (req, res) => {
    try {
        // Get session ID from header, cookie, or query
        const sessionId = req.headers['x-session-id'] ||
            req.headers['X-Session-ID'] ||
            req.cookies?.sessionId ||
            req.query?.sessionId;

        if (sessionId) {
            const destroyed = sessionManager.destroySession(sessionId);
            if (destroyed) {
                return res.json({
                    success: true,
                    message: 'Đăng xuất thành công'
                });
            }
        }

        // Also support backward compatibility with user-id header
        // If no session found but user-id is provided, just return success
        const userId = req.headers['user-id'] || req.body.userId;
        if (userId) {
            return res.json({
                success: true,
                message: 'Đăng xuất thành công'
            });
        }

        // If neither session nor user-id provided
        res.json({
            success: true,
            message: 'Đăng xuất thành công'
        });
    } catch (error) {
        console.error('Logout error:', error);
        res.status(500).json({
            success: false,
            error: 'Server error'
        });
    }
});

module.exports = router;

