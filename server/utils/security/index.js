/**
 * Chỉ mục tiện ích bảo mật
 * Export tập trung cho tất cả các module bảo mật
 */

const { rateLimiter, createRateLimitMiddleware, rateLimiters } = require('./rateLimiter');
const InputValidator = require('./InputValidator');
const { sessionManager, sessionMiddleware } = require('./SessionManager');

module.exports = {
    rateLimiter,
    createRateLimitMiddleware,
    rateLimiters,
    InputValidator,
    sessionManager,
    sessionMiddleware
};
