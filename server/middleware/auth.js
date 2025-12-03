const mongoose = require('mongoose');

// Middleware để xác thực user
// userId có thể từ body, query params, hoặc headers
function authenticateUser(req, res, next) {
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
