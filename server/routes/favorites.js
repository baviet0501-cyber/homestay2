const express = require('express');
const router = express.Router();
const Favorite = require('../models/Favorite');
const Room = require('../models/Room');
const { authenticateUser } = require('../middleware/auth');
const mongoose = require('mongoose');

// GET /api/favorites?userId=:userId
router.get('/', authenticateUser, async (req, res) => {
    try {
        const userId = req.userId;
        
        if (!mongoose.Types.ObjectId.isValid(userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        const favorites = await Favorite.find({ userId })
            .sort({ createdAt: -1 })
            .populate('roomId');

        const favoritesResponse = favorites.map(fav => ({
            id: fav._id.toString(),
            userId: fav.userId.toString(),
            roomId: fav.roomId._id.toString(),
            room: {
                id: fav.roomId._id.toString(),
                name: fav.roomId.name,
                description: fav.roomId.description,
                price: fav.roomId.price,
                capacity: fav.roomId.capacity,
                imageUrl: fav.roomId.imageUrl,
                maxSlots: fav.roomId.maxSlots,
                createdAt: fav.roomId.createdAt.getTime()
            },
            createdAt: fav.createdAt.getTime()
        }));

        res.json(favoritesResponse);
    } catch (error) {
        console.error('Get favorites error:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

// POST /api/favorites
router.post('/', authenticateUser, async (req, res) => {
    try {
        const { roomId } = req.body;

        if (!roomId) {
            return res.status(400).json({ error: 'Room ID is required' });
        }

        if (!mongoose.Types.ObjectId.isValid(req.userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        if (!mongoose.Types.ObjectId.isValid(roomId)) {
            return res.status(400).json({ error: 'Invalid room ID' });
        }

        // Check if favorite already exists
        const existing = await Favorite.findOne({ userId: req.userId, roomId });
        if (existing) {
            return res.status(400).json({ error: 'Favorite already exists' });
        }

        const favorite = new Favorite({
            userId: req.userId,
            roomId
        });

        await favorite.save();

        res.status(201).json({
            success: true,
            favorite: {
                id: favorite._id.toString(),
                userId: favorite.userId.toString(),
                roomId: favorite.roomId.toString(),
                createdAt: favorite.createdAt.getTime()
            }
        });
    } catch (error) {
        console.error('Add favorite error:', error);
        if (error.code === 11000) {
            return res.status(400).json({ error: 'Favorite already exists' });
        }
        res.status(500).json({ error: 'Failed to add favorite' });
    }
});

// DELETE /api/favorites/:id
router.delete('/:id', authenticateUser, async (req, res) => {
    try {
        const favoriteId = req.params.id;

        if (!mongoose.Types.ObjectId.isValid(favoriteId)) {
            return res.status(400).json({ error: 'Invalid favorite ID' });
        }

        // Check if favorite exists and belongs to user
        const favorite = await Favorite.findOne({ _id: favoriteId, userId: req.userId });
        if (!favorite) {
            return res.status(404).json({ error: 'Favorite not found' });
        }

        await Favorite.deleteOne({ _id: favoriteId });

        res.json({
            success: true,
            message: 'Favorite deleted successfully'
        });
    } catch (error) {
        console.error('Delete favorite error:', error);
        res.status(500).json({ error: 'Failed to delete favorite' });
    }
});

// DELETE /api/favorites?userId=:userId&roomId=:roomId
router.delete('/', authenticateUser, async (req, res) => {
    try {
        const { roomId } = req.query;

        if (!roomId) {
            return res.status(400).json({ error: 'Room ID is required' });
        }

        if (!mongoose.Types.ObjectId.isValid(roomId)) {
            return res.status(400).json({ error: 'Invalid room ID' });
        }

        const result = await Favorite.deleteOne({ userId: req.userId, roomId });

        if (result.deletedCount === 0) {
            return res.status(404).json({ error: 'Favorite not found' });
        }

        res.json({
            success: true,
            message: 'Favorite deleted successfully'
        });
    } catch (error) {
        console.error('Delete favorite error:', error);
        res.status(500).json({ error: 'Database error' });
    }
});

module.exports = router;
