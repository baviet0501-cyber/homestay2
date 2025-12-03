const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const Admin = require('../models/Admin');
const User = require('../models/User');
const Room = require('../models/Room');
const Booking = require('../models/Booking');

// ==================== ADMIN AUTH ====================

/**
 * POST /api/admin/login
 * Admin login
 */
router.post('/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required' });
        }

        // Find admin by username
        const admin = await Admin.findOne({ username });
        if (!admin) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        // Check if admin is active
        if (!admin.isActive) {
            return res.status(403).json({ error: 'Account is deactivated' });
        }

        // Verify password
        const isValid = await bcrypt.compare(password, admin.password);
        if (!isValid) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        // Return admin info (without password)
        res.json({
            success: true,
            admin: {
                id: admin._id.toString(),
                username: admin.username,
                fullName: admin.fullName,
                email: admin.email,
                role: admin.role,
                createdAt: admin.createdAt.getTime()
            }
        });
    } catch (error) {
        console.error('Admin login error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

// ==================== USER MANAGEMENT ====================

/**
 * GET /api/admin/users
 * Get all users
 */
router.get('/users', async (req, res) => {
    try {
        const users = await User.find().select('-password').sort({ createdAt: -1 });
        
        res.json({
            success: true,
            users: users.map(user => ({
                id: user._id.toString(),
                email: user.email,
                phone: user.phone,
                fullName: user.fullName,
                createdAt: user.createdAt.getTime()
            }))
        });
    } catch (error) {
        console.error('Get users error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * DELETE /api/admin/users/:id
 * Delete user
 */
router.delete('/users/:id', async (req, res) => {
    try {
        const userId = req.params.id;
        
        const user = await User.findByIdAndDelete(userId);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        res.json({
            success: true,
            message: 'User deleted successfully'
        });
    } catch (error) {
        console.error('Delete user error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

// ==================== ROOM MANAGEMENT ====================

/**
 * GET /api/admin/rooms
 * Get all rooms
 */
router.get('/rooms', async (req, res) => {
    try {
        const rooms = await Room.find().sort({ createdAt: -1 });
        
        res.json({
            success: true,
            rooms: rooms.map(room => ({
                id: room._id.toString(),
                name: room.name,
                description: room.description,
                price: room.price,
                capacity: room.capacity,
                imageUrl: room.imageUrl,
                maxSlots: room.maxSlots,
                createdAt: room.createdAt.getTime()
            }))
        });
    } catch (error) {
        console.error('Get rooms error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * POST /api/admin/rooms
 * Create new room
 */
router.post('/rooms', async (req, res) => {
    try {
        const { name, description, price, capacity, imageUrl, maxSlots } = req.body;

        if (!name || !price || !capacity) {
            return res.status(400).json({ error: 'Name, price and capacity are required' });
        }

        const room = new Room({
            name,
            description: description || '',
            price,
            capacity,
            imageUrl: imageUrl || '',
            maxSlots: maxSlots || 1
        });

        await room.save();

        res.status(201).json({
            success: true,
            room: {
                id: room._id.toString(),
                name: room.name,
                description: room.description,
                price: room.price,
                capacity: room.capacity,
                imageUrl: room.imageUrl,
                maxSlots: room.maxSlots,
                createdAt: room.createdAt.getTime()
            }
        });
    } catch (error) {
        console.error('Create room error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * PUT /api/admin/rooms/:id
 * Update room
 */
router.put('/rooms/:id', async (req, res) => {
    try {
        const roomId = req.params.id;
        const { name, description, price, capacity, imageUrl, maxSlots } = req.body;

        const room = await Room.findById(roomId);
        if (!room) {
            return res.status(404).json({ error: 'Room not found' });
        }

        // Update fields
        if (name) room.name = name;
        if (description !== undefined) room.description = description;
        if (price) room.price = price;
        if (capacity) room.capacity = capacity;
        if (imageUrl !== undefined) room.imageUrl = imageUrl;
        if (maxSlots) room.maxSlots = maxSlots;

        await room.save();

        res.json({
            success: true,
            room: {
                id: room._id.toString(),
                name: room.name,
                description: room.description,
                price: room.price,
                capacity: room.capacity,
                imageUrl: room.imageUrl,
                maxSlots: room.maxSlots,
                createdAt: room.createdAt.getTime()
            }
        });
    } catch (error) {
        console.error('Update room error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * DELETE /api/admin/rooms/:id
 * Delete room
 */
router.delete('/rooms/:id', async (req, res) => {
    try {
        const roomId = req.params.id;
        
        const room = await Room.findByIdAndDelete(roomId);
        if (!room) {
            return res.status(404).json({ error: 'Room not found' });
        }

        res.json({
            success: true,
            message: 'Room deleted successfully'
        });
    } catch (error) {
        console.error('Delete room error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

// ==================== BOOKING MANAGEMENT ====================

/**
 * GET /api/admin/bookings
 * Get all bookings with user and room info
 */
router.get('/bookings', async (req, res) => {
    try {
        const bookings = await Booking.find()
            .populate('userId', 'email fullName phone')
            .populate('roomId', 'name price')
            .sort({ createdAt: -1 });
        
        res.json({
            success: true,
            bookings: bookings.map(booking => ({
                id: booking._id.toString(),
                user: booking.userId ? {
                    id: booking.userId._id.toString(),
                    email: booking.userId.email,
                    fullName: booking.userId.fullName,
                    phone: booking.userId.phone
                } : null,
                room: booking.roomId ? {
                    id: booking.roomId._id.toString(),
                    name: booking.roomId.name,
                    price: booking.roomId.price
                } : null,
                checkInDate: booking.checkInDate.getTime(),
                checkOutDate: booking.checkOutDate.getTime(),
                guestCount: booking.guestCount,
                totalPrice: booking.totalPrice,
                status: booking.status,
                paymentMethod: booking.paymentMethod,
                createdAt: booking.createdAt.getTime()
            }))
        });
    } catch (error) {
        console.error('Get bookings error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * PUT /api/admin/bookings/:id/status
 * Update booking status
 */
router.put('/bookings/:id/status', async (req, res) => {
    try {
        const bookingId = req.params.id;
        const { status } = req.body;

        if (!status) {
            return res.status(400).json({ error: 'Status is required' });
        }

        const validStatuses = ['pending', 'confirmed', 'cancelled', 'completed'];
        if (!validStatuses.includes(status)) {
            return res.status(400).json({ error: 'Invalid status' });
        }

        const booking = await Booking.findById(bookingId);
        if (!booking) {
            return res.status(404).json({ error: 'Booking not found' });
        }

        booking.status = status;
        await booking.save();

        res.json({
            success: true,
            booking: {
                id: booking._id.toString(),
                status: booking.status
            }
        });
    } catch (error) {
        console.error('Update booking status error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * DELETE /api/admin/bookings/:id
 * Delete booking
 */
router.delete('/bookings/:id', async (req, res) => {
    try {
        const bookingId = req.params.id;
        
        const booking = await Booking.findByIdAndDelete(bookingId);
        if (!booking) {
            return res.status(404).json({ error: 'Booking not found' });
        }

        res.json({
            success: true,
            message: 'Booking deleted successfully'
        });
    } catch (error) {
        console.error('Delete booking error:', error);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;

