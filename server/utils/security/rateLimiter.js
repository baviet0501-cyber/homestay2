/**
 * Rate Limiter - Ngăn chặn tấn công brute force và lạm dụng API
 * Sử dụng bộ nhớ trong (đối với production, nên cân nhắc dùng Redis)
 */

class RateLimiter {
    constructor() {
        // Kho lưu trữ: { key: { count: number, resetTime: number } }
        this.store = new Map();
        this.cleanupInterval = null;

        // Bắt đầu interval dọn dẹp để xóa các mục hết hạn
        this.startCleanup();
    }

    /**
     * Kiểm tra xem yêu cầu có được phép không
     * @param {string} key - Định danh duy nhất (IP, user ID, v.v.)
     * @param {number} maxRequests - Số lượng yêu cầu tối đa cho phép
     * @param {number} windowMs - Cửa sổ thời gian tính bằng mili giây
     * @returns {Object} - { allowed: boolean, remaining: number, resetTime: number }
     */
    check(key, maxRequests = 100, windowMs = 60000) {
        const now = Date.now();
        const record = this.store.get(key);

        if (!record || now > record.resetTime) {
            // Tạo bản ghi mới hoặc reset bản ghi đã hết hạn
            this.store.set(key, {
                count: 1,
                resetTime: now + windowMs
            });
            return {
                allowed: true,
                remaining: maxRequests - 1,
                resetTime: now + windowMs
            };
        }

        if (record.count >= maxRequests) {
            return {
                allowed: false,
                remaining: 0,
                resetTime: record.resetTime
            };
        }

        // Tăng số đếm

        record.count++;
        return {
            allowed: true,
            remaining: maxRequests - record.count,
            resetTime: record.resetTime
        };
    }

    /**
     * Lấy thông tin giới hạn tốc độ mà không tăng số đếm
     * @param {string} key - Định danh duy nhất
     * @param {number} maxRequests - Số lượng yêu cầu tối đa cho phép
     * @param {number} windowMs - Cửa sổ thời gian tính bằng mili giây
     * @returns {Object} - { remaining: number, resetTime: number }
     */
    getInfo(key, maxRequests = 100, windowMs = 60000) {
        const now = Date.now();
        const record = this.store.get(key);

        if (!record || now > record.resetTime) {
            return {
                remaining: maxRequests,
                resetTime: now + windowMs
            };
        }

        return {
            remaining: Math.max(0, maxRequests - record.count),
            resetTime: record.resetTime
        };
    }

    /**
     * Reset giới hạn tốc độ cho một key
     * @param {string} key - Key cần reset
     */
    reset(key) {
        this.store.delete(key);
    }

    /**
     * Xóa tất cả các bản ghi giới hạn tốc độ
     */
    clear() {
        this.store.clear();
    }

    /**
     * Bắt đầu interval dọn dẹp để xóa các mục hết hạn
     */
    startCleanup() {
        // Dọn dẹp các mục hết hạn mỗi 5 phút
        this.cleanupInterval = setInterval(() => {
            const now = Date.now();
            for (const [key, record] of this.store.entries()) {
                if (now > record.resetTime) {
                    this.store.delete(key);
                }
            }
        }, 5 * 60 * 1000);
    }

    /**
     * Dừng interval dọn dẹp
     */
    stopCleanup() {
        if (this.cleanupInterval) {
            clearInterval(this.cleanupInterval);
            this.cleanupInterval = null;
        }
    }
}

// Tạo instance singleton
const rateLimiter = new RateLimiter();

/**
 * Express middleware cho giới hạn tốc độ
 * @param {Object} options - Các tùy chọn giới hạn tốc độ
 * @param {number} options.maxRequests - Số lượng yêu cầu tối đa (mặc định: 100)
 * @param {number} options.windowMs - Cửa sổ thời gian tính bằng mili giây (mặc định: 60000 = 1 phút)
 * @param {Function} options.keyGenerator - Hàm tạo key từ request (mặc định: sử dụng IP)
 * @param {Function} options.onLimitReached - Callback khi đạt giới hạn
 * @returns {Function} - Express middleware
 */
function createRateLimitMiddleware(options = {}) {
    const {
        maxRequests = 100,
        windowMs = 60000,
        keyGenerator = (req) => req.ip || req.connection.remoteAddress,
        onLimitReached = null
    } = options;

    return (req, res, next) => {
        const key = keyGenerator(req);
        const result = rateLimiter.check(key, maxRequests, windowMs);

        // Thiết lập các header giới hạn tốc độ
        res.setHeader('X-RateLimit-Limit', maxRequests);
        res.setHeader('X-RateLimit-Remaining', result.remaining);
        res.setHeader('X-RateLimit-Reset', new Date(result.resetTime).toISOString());

        if (!result.allowed) {
            if (onLimitReached) {
                onLimitReached(req, res);
            }
            return res.status(429).json({
                success: false,
                error: 'Too many requests. Please try again later.',
                retryAfter: Math.ceil((result.resetTime - Date.now()) / 1000)
            });
        }

        next();
    };
}

/**
 * Các bộ giới hạn tốc độ được cấu hình sẵn cho các endpoint khác nhau
 */
const rateLimiters = {
    // Bộ giới hạn nghiêm ngặt cho các endpoint xác thực (đăng nhập, đăng ký)
    auth: createRateLimitMiddleware({
        maxRequests: 5,
        windowMs: 100 * 365 * 24 * 60 * 60 * 1000, // 100 năm (Khóa vĩnh viễn)
        keyGenerator: (req) => `auth:${req.ip || req.connection.remoteAddress}`
    }),

    // Bộ giới hạn tốc độ API chung
    general: createRateLimitMiddleware({
        maxRequests: 100,
        windowMs: 60000 // 1 minute
    }),

    // Bộ giới hạn nghiêm ngặt cho các thao tác nhạy cảm
    strict: createRateLimitMiddleware({
        maxRequests: 10,
        windowMs: 60000 // 1 minute
    })
};

module.exports = {
    RateLimiter,
    rateLimiter,
    createRateLimitMiddleware,
    rateLimiters
};

