const mongoose = require('mongoose');
const { sessionManager } = require('../utils/security');

// Middleware để xác thực user
// Ưu tiên session, sau đó mới kiểm tra user-id header (backward compatible)
function authenticateUser(req, res, next) {
    // First, try to get userId from session
    const sessionId = req.headers['x-session-id'] || 
                     req.headers['X-Session-ID'] ||
                     req.cookies?.sessionId ||
                     req.query?.sessionId;
    
    if (sessionId) {
        const userId = sessionManager.verifySession(sessionId);
        if (userId) {
            // Session valid - set userId and session info
            req.userId = userId;
            req.sessionId = sessionId;
            req.session = sessionManager.getSession(sessionId);
            return next();
        }
    }
    
    // Fallback to user-id header (backward compatible)
    let userId = req.body.userId || req.query.userId || req.headers['user-id'];
    
    // Convert string to ObjectId if needed
    if (userId && typeof userId === 'string' && mongoose.Types.ObjectId.isValid(userId)) {
        req.userId = userId;
    } else if (userId && typeof userId === 'number') {
        // For backwards compatibility with numeric IDs, convert to string
        req.userId = userId.toString();
    } else {
        return res.status(401).json({ error: 'User ID required' });
    }
    
    next();
}

// Middleware optional - không bắt buộc userId
function optionalAuth(req, res, next) {
    // First, try to get userId from session
    const sessionId = req.headers['x-session-id'] || 
                     req.headers['X-Session-ID'] ||
                     req.cookies?.sessionId ||
                     req.query?.sessionId;
    
    if (sessionId) {
        const userId = sessionManager.verifySession(sessionId);
        if (userId) {
            req.userId = userId;
            req.sessionId = sessionId;
            req.session = sessionManager.getSession(sessionId);
            return next();
        }
    }
    
    // Fallback to user-id header (backward compatible)
    let userId = req.body.userId || req.query.userId || req.headers['user-id'];
    
    if (userId && mongoose.Types.ObjectId.isValid(userId)) {
        req.userId = userId;
    } else {
        req.userId = null;
    }
    
    next();
}

module.exports = {
    authenticateUser,
    optionalAuth
};
