const express = require('express');
const router = express.Router();
const Booking = require('../models/Booking');
const { authenticateUser } = require('../middleware/auth');
const mongoose = require('mongoose');

// GET /api/bookings?userId=:userId
router.get('/', authenticateUser, async (req, res) => {
    try {
        const userId = req.userId;
        
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        const bookings = await Booking.find({ userId })
            .sort({ createdAt: -1 })
            .populate('roomId', 'name description price imageUrl')
            .populate('slotId', 'name price');

        const bookingsResponse = bookings.map(booking => ({
            id: booking._id.toString(),
            userId: booking.userId.toString(),
            roomId: booking.roomId._id.toString(),
            checkInDate: new Date(booking.checkInDate).getTime(),
            checkOutDate: new Date(booking.checkOutDate).getTime(),
            guestCount: booking.guestCount,
            totalPrice: booking.totalPrice,
            status: booking.status,
            paymentMethod: booking.paymentMethod,
            slotId: booking.slotId ? booking.slotId._id.toString() : null,
            createdAt: booking.createdAt.getTime()
        }));

        res.json(bookingsResponse);
    } catch (error) {
        console.error('Get bookings error:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

// POST /api/bookings
router.post('/', authenticateUser, async (req, res) => {
    try {
        const {
            roomId,
            checkInDate,
            checkOutDate,
            guestCount,
            totalPrice,
            status = 'pending',
            paymentMethod,
            slotId
        } = req.body;

        if (!roomId || !checkInDate || !checkOutDate || !guestCount || !totalPrice) {
            return res.status(400).json({ error: 'Missing required fields' });
        }

        if (!mongoose.Types.ObjectId.isValid(req.userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        if (!mongoose.Types.ObjectId.isValid(roomId)) {
            return res.status(400).json({ error: 'Invalid room ID' });
        }

        const booking = new Booking({
            userId: req.userId,
            roomId,
            checkInDate: new Date(checkInDate),
            checkOutDate: new Date(checkOutDate),
            guestCount,
            totalPrice,
            status,
            paymentMethod,
            slotId: slotId && mongoose.Types.ObjectId.isValid(slotId) ? slotId : null
        });

        await booking.save();

        res.status(201).json({
            success: true,
            booking: {
                id: booking._id.toString(),
                userId: booking.userId.toString(),
                roomId: booking.roomId.toString(),
                checkInDate: booking.checkInDate.getTime(),
                checkOutDate: booking.checkOutDate.getTime(),
                guestCount: booking.guestCount,
                totalPrice: booking.totalPrice,
                status: booking.status,
                paymentMethod: booking.paymentMethod,
                slotId: booking.slotId ? booking.slotId.toString() : null,
                createdAt: booking.createdAt.getTime()
            }
        });
    } catch (error) {
        console.error('Create booking error:', error);
        res.status(500).json({ error: 'Failed to create booking' });
    }
});

// PUT /api/bookings/:id
router.put('/:id', authenticateUser, async (req, res) => {
    try {
        const bookingId = req.params.id;
        const { status, paymentMethod } = req.body;

        if (!mongoose.Types.ObjectId.isValid(bookingId)) {
            return res.status(400).json({ error: 'Invalid booking ID' });
        }

        // Check if booking exists and belongs to user
        const booking = await Booking.findOne({ _id: bookingId, userId: req.userId });
        if (!booking) {
            return res.status(404).json({ error: 'Booking not found' });
        }

        if (status) booking.status = status;
        if (paymentMethod !== undefined) booking.paymentMethod = paymentMethod;

        await booking.save();

        res.json({
            success: true,
            message: 'Booking updated successfully'
        });
    } catch (error) {
        console.error('Update booking error:', error);
        res.status(500).json({ error: 'Failed to update booking' });
    }
});

module.exports = router;
