/**
 * Script để khóa một user thủ công (dùng cho testing)
 * Usage: node scripts/lockUser.js <email>
 */

require('dotenv').config();
const mongoose = require('mongoose');
const User = require('../models/User');

async function lockUser(email) {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/homestay');
        console.log('Connected to MongoDB');

        // Find user
        const user = await User.findOne({ email: email.toLowerCase().trim() });
        if (!user) {
            console.error(`User with email ${email} not found`);
            process.exit(1);
        }

        // Lock user permanently
        const now = Date.now();
        // Khóa vĩnh viễn: set lockedUntil thành 100 năm sau
        const PERMANENT_LOCK_DATE = new Date(now + 100 * 365 * 24 * 60 * 60 * 1000); // 100 năm sau
        user.failedLoginAttempts = 5;
        user.lockedUntil = PERMANENT_LOCK_DATE;
        user.lastLoginAttempt = new Date();

        await user.save();

        console.log(`✅ User ${email} has been locked PERMANENTLY`);
        console.log(`   Failed attempts: ${user.failedLoginAttempts}`);
        console.log(`   Locked until: ${user.lockedUntil}`);
        console.log(`   Status: PERMANENT LOCK (vĩnh viễn)`);

        await mongoose.connection.close();
        process.exit(0);
    } catch (error) {
        console.error('Error:', error);
        process.exit(1);
    }
}

// Get email from command line
const email = process.argv[2];
if (!email) {
    console.error('Usage: node scripts/lockUser.js <email>');
    process.exit(1);
}

lockUser(email);

