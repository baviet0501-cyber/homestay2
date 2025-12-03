/**
 * Session Manager - Quản lý phiên người dùng và token
 * Cung cấp xử lý phiên an toàn mà không cần thư viện bên ngoài
 * 
 * Tính năng:
 * - Session ID ngẫu nhiên an toàn (64 ký tự hex)
 * - Tự động hết hạn phiên (mặc định 24 giờ)
 * - Dọn dẹp phiên (xóa phiên hết hạn mỗi giờ)
 * - Tối đa 5 phiên đồng thời mỗi người dùng
 * - Theo dõi địa chỉ IP và User Agent để bảo mật
 */

const crypto = require('crypto');

class SessionManager {
    /**
     * Khởi tạo SessionManager với cấu hình mặc định
     */
    constructor() {
        // Kho lưu trữ: { sessionId: { userId, createdAt, expiresAt, ip, userAgent, lastActivity } }
        this.sessions = new Map();
        this.cleanupInterval = null;

        // Cấu hình phiên
        this.config = {
            sessionDuration: 24 * 60 * 60 * 1000,      // 24 giờ
            cleanupInterval: 60 * 60 * 1000,          // 1 giờ
            maxSessionsPerUser: 5                      // Số phiên đồng thời tối đa mỗi người dùng
        };

        // Bắt đầu dọn dẹp tự động
        this.startCleanup();
    }

    /**
     * Tạo session ID ngẫu nhiên an toàn
     * @returns {string} - Chuỗi hex 64 ký tự (32 bytes)
     */
    generateSessionId() {
        return crypto.randomBytes(32).toString('hex');
    }

    /**
     * Tạo phiên mới cho người dùng
     * @param {string} userId - User ID (MongoDB ObjectId)
     * @param {Object} options - Tùy chọn phiên
     * @param {string} [options.ip] - Địa chỉ IP của client
     * @param {string} [options.userAgent] - User agent của client
     * @param {number} [options.duration] - Thời lượng phiên tính bằng mili giây (mặc định: 24 giờ)
     * @returns {string} - Session ID (64 ký tự hex)
     */
    createSession(userId, options = {}) {
        const {
            ip = null,
            userAgent = null,
            duration = this.config.sessionDuration
        } = options;

        // Dọn dẹp các phiên cũ của người dùng này nếu đạt giới hạn
        this.cleanupUserSessions(userId);

        // Tạo session ID mới
        const sessionId = this.generateSessionId();
        const now = Date.now();

        // Lưu trữ dữ liệu phiên
        this.sessions.set(sessionId, {
            userId,
            createdAt: now,
            expiresAt: now + duration,
            ip,
            userAgent,
            lastActivity: now
        });

        return sessionId;
    }

    /**
     * Lấy dữ liệu phiên theo session ID
     * Tự động cập nhật lastActivity và xóa phiên hết hạn
     * @param {string} sessionId - Session ID
     * @returns {Object|null} - Dữ liệu phiên hoặc null nếu không tìm thấy/không hợp lệ/hết hạn
     */
    getSession(sessionId) {
        if (!sessionId) {
            return null;
        }

        const session = this.sessions.get(sessionId);
        if (!session) {
            return null;
        }

        // Kiểm tra xem phiên đã hết hạn chưa
        const now = Date.now();
        if (now > session.expiresAt) {
            this.sessions.delete(sessionId);
            return null;
        }

        // Cập nhật hoạt động gần nhất
        session.lastActivity = now;

        return {
            userId: session.userId,
            createdAt: session.createdAt,
            expiresAt: session.expiresAt,
            ip: session.ip,
            userAgent: session.userAgent
        };
    }

    /**
     * Xác minh phiên và trả về user ID
     * @param {string} sessionId - Session ID
     * @returns {string|null} - User ID nếu phiên hợp lệ, null nếu không
     */
    verifySession(sessionId) {
        const session = this.getSession(sessionId);
        return session ? session.userId : null;
    }

    /**
     * Hủy một phiên
     * @param {string} sessionId - Session ID cần hủy
     * @returns {boolean} - True nếu phiên được tìm thấy và hủy
     */
    destroySession(sessionId) {
        if (!sessionId) {
            return false;
        }
        return this.sessions.delete(sessionId);
    }

    /**
     * Hủy tất cả phiên của một người dùng
     * @param {string} userId - User ID
     * @returns {number} - Số phiên đã hủy
     */
    destroyUserSessions(userId) {
        if (!userId) {
            return 0;
        }

        let count = 0;
        for (const [sessionId, session] of this.sessions.entries()) {
            if (session.userId === userId) {
                this.sessions.delete(sessionId);
                count++;
            }
        }
        return count;
    }

    /**
     * Dọn dẹp các phiên cũ của người dùng nếu đạt giới hạn
     * Xóa các phiên cũ nhất trước (dựa trên lastActivity)
     * @param {string} userId - User ID
     * @private
     */
    cleanupUserSessions(userId) {
        if (!userId) {
            return;
        }

        const userSessions = [];

        // Thu thập tất cả phiên của người dùng này
        for (const [sessionId, session] of this.sessions.entries()) {
            if (session.userId === userId) {
                userSessions.push({
                    sessionId,
                    lastActivity: session.lastActivity
                });
            }
        }

        // Nếu đạt giới hạn, xóa các phiên cũ nhất
        if (userSessions.length >= this.config.maxSessionsPerUser) {
            // Sắp xếp theo lastActivity (cũ nhất trước)
            userSessions.sort((a, b) => a.lastActivity - b.lastActivity);

            // Tính số phiên cần xóa
            const toRemove = userSessions.length - this.config.maxSessionsPerUser + 1;

            // Xóa các phiên cũ nhất
            for (let i = 0; i < toRemove; i++) {
                this.sessions.delete(userSessions[i].sessionId);
            }
        }
    }

    /**
     * Gia hạn thời gian hết hạn phiên
     * @param {string} sessionId - Session ID
     * @param {number} [duration] - Thời gian gia hạn thêm tính bằng mili giây (mặc định: 24 giờ)
     * @returns {boolean} - True nếu phiên được gia hạn, false nếu không tìm thấy
     */
    extendSession(sessionId, duration = this.config.sessionDuration) {
        if (!sessionId) {
            return false;
        }

        const session = this.sessions.get(sessionId);
        if (!session) {
            return false;
        }

        const now = Date.now();
        session.expiresAt = now + duration;
        session.lastActivity = now;
        return true;
    }

    /**
     * Lấy tất cả phiên đang hoạt động của một người dùng
     * @param {string} userId - User ID
     * @returns {Array} - Mảng các đối tượng phiên (không có userId vì lý do bảo mật)
     */
    getUserSessions(userId) {
        if (!userId) {
            return [];
        }

        const sessions = [];
        const now = Date.now();

        for (const [sessionId, session] of this.sessions.entries()) {
            if (session.userId === userId && now <= session.expiresAt) {
                sessions.push({
                    sessionId,
                    createdAt: session.createdAt,
                    expiresAt: session.expiresAt,
                    lastActivity: session.lastActivity,
                    ip: session.ip,
                    userAgent: session.userAgent
                });
            }
        }

        return sessions;
    }

    /**
     * Bắt đầu interval dọn dẹp tự động
     * Xóa các phiên hết hạn định kỳ
     * @private
     */
    startCleanup() {
        if (this.cleanupInterval) {
            this.stopCleanup();
        }

        this.cleanupInterval = setInterval(() => {
            this.cleanup();
        }, this.config.cleanupInterval);
    }

    /**
     * Dừng interval dọn dẹp tự động
     */
    stopCleanup() {
        if (this.cleanupInterval) {
            clearInterval(this.cleanupInterval);
            this.cleanupInterval = null;
        }
    }

    /**
     * Dọn dẹp các phiên hết hạn
     * @returns {number} - Số phiên đã xóa
     * @private
     */
    cleanup() {
        const now = Date.now();
        let count = 0;

        for (const [sessionId, session] of this.sessions.entries()) {
            if (now > session.expiresAt) {
                this.sessions.delete(sessionId);
                count++;
            }
        }

        return count;
    }

    /**
     * Lấy thống kê phiên
     * @returns {Object} - Đối tượng thống kê
     */
    getStats() {
        const now = Date.now();
        let active = 0;
        let expired = 0;
        const users = new Set();

        for (const session of this.sessions.values()) {
            if (now <= session.expiresAt) {
                active++;
                users.add(session.userId);
            } else {
                expired++;
            }
        }

        return {
            total: this.sessions.size,
            active,
            expired,
            uniqueUsers: users.size
        };
    }
}

// Tạo instance singleton
const sessionManager = new SessionManager();

/**
 * Express middleware để trích xuất và xác minh phiên
 * Hỗ trợ nhiều cách cung cấp session ID:
 * - Header: X-Session-ID
 * - Cookie: sessionId
 * - Query parameter: ?sessionId=...
 * 
 * @param {Object} options - Tùy chọn middleware
 * @param {string} [options.headerName='X-Session-ID'] - Tên header cho session ID
 * @param {boolean} [options.required=false] - Phiên có bắt buộc không (trả về 401 nếu thiếu)
 * @returns {Function} - Hàm Express middleware
 */
function sessionMiddleware(options = {}) {
    const {
        headerName = 'X-Session-ID',
        required = false
    } = options;

    return (req, res, next) => {
        // Thử lấy session ID từ nhiều nguồn
        const sessionId = req.headers[headerName.toLowerCase()] ||
            req.headers[headerName] ||
            req.cookies?.sessionId ||
            req.query?.sessionId;

        // Nếu không có session ID
        if (!sessionId) {
            if (required) {
                return res.status(401).json({
                    success: false,
                    error: 'Yêu cầu phiên đăng nhập'
                });
            }
            // Tùy chọn: đặt null và tiếp tục
            req.session = null;
            req.userId = null;
            return next();
        }

        // Xác minh phiên
        const userId = sessionManager.verifySession(sessionId);

        if (!userId) {
            if (required) {
                return res.status(401).json({
                    success: false,
                    error: 'Phiên không hợp lệ hoặc đã hết hạn'
                });
            }
            // Tùy chọn: đặt null và tiếp tục
            req.session = null;
            req.userId = null;
            return next();
        }

        // Phiên hợp lệ: gắn vào request
        req.session = sessionManager.getSession(sessionId);
        req.userId = userId;
        req.sessionId = sessionId;

        next();
    };
}

module.exports = {
    SessionManager,
    sessionManager,
    sessionMiddleware
};
