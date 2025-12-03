const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const User = require('../models/User');
const { authenticateUser } = require('../middleware/auth');
const mongoose = require('mongoose');

// GET /api/users/:id
router.get('/:id', async (req, res) => {
    try {
        const userId = req.params.id;
        
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        const user = await User.findById(userId).select('-password');
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        res.json({
            id: user._id.toString(),
            email: user.email,
            phone: user.phone,
            fullName: user.fullName,
            createdAt: user.createdAt.getTime()
        });
    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

// PUT /api/users/:id
// Update user profile - CHỈ cho phép update fullName và password
router.put('/:id', async (req, res) => {
    try {
        const userId = req.params.id;
        
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        const { fullName, password } = req.body;

        // Get current user
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // Update fields (CHỈ fullName và password, KHÔNG cho phép đổi email/phone)
        if (fullName) {
            user.fullName = fullName;
        }
        
        // Hash new password if provided
        if (password) {
            user.password = await bcrypt.hash(password, 10);
        }

        await user.save();

        // Return updated user info (without password)
        res.json({
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
        console.error('Update user error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
