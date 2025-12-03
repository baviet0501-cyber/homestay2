const express = require('express');
const router = express.Router();
const Room = require('../models/Room');
const mongoose = require('mongoose');

// GET /api/rooms
router.get('/', async (req, res) => {
    try {
        const rooms = await Room.find().sort({ createdAt: -1 });
        
        const roomsResponse = rooms.map(room => ({
            id: room._id.toString(),
            name: room.name,
            description: room.description,
            price: room.price,
            capacity: room.capacity,
            imageUrl: room.imageUrl,
            maxSlots: room.maxSlots,
            location: room.location || '',
            address: room.address || '',
            rating: room.rating || 0,
            reviewCount: room.reviewCount || 0,
            amenities: room.amenities || '',
            roomType: room.roomType || '',
            area: room.area || 0,
            createdAt: room.createdAt.getTime()
        }));

        res.json(roomsResponse);
    } catch (error) {
        console.error('Get rooms error:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

// GET /api/rooms/search?q=query
router.get('/search', async (req, res) => {
    try {
        const query = req.query.q || '';
        const searchRegex = new RegExp(query, 'i');

        const rooms = await Room.find({
            $or: [
                { name: searchRegex },
                { description: searchRegex }
            ]
        }).sort({ createdAt: -1 });

        const roomsResponse = rooms.map(room => ({
            id: room._id.toString(),
            name: room.name,
            description: room.description,
            price: room.price,
            capacity: room.capacity,
            imageUrl: room.imageUrl,
            maxSlots: room.maxSlots,
            location: room.location || '',
            address: room.address || '',
            rating: room.rating || 0,
            reviewCount: room.reviewCount || 0,
            amenities: room.amenities || '',
            roomType: room.roomType || '',
            area: room.area || 0,
            createdAt: room.createdAt.getTime()
        }));

        res.json(roomsResponse);
    } catch (error) {
        console.error('Search rooms error:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

// GET /api/rooms/:id
router.get('/:id', async (req, res) => {
    try {
        const roomId = req.params.id;
        
        if (!mongoose.Types.ObjectId.isValid(roomId)) {
            return res.status(400).json({ error: 'Invalid room ID' });
        }

        const room = await Room.findById(roomId);
        if (!room) {
            return res.status(404).json({ error: 'Room not found' });
        }

        res.json({
            id: room._id.toString(),
            name: room.name,
            description: room.description,
            price: room.price,
            capacity: room.capacity,
            imageUrl: room.imageUrl,
            maxSlots: room.maxSlots,
            location: room.location || '',
            address: room.address || '',
            rating: room.rating || 0,
            reviewCount: room.reviewCount || 0,
            amenities: room.amenities || '',
            roomType: room.roomType || '',
            area: room.area || 0,
            createdAt: room.createdAt.getTime()
        });
    } catch (error) {
        console.error('Get room error:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

module.exports = router;
