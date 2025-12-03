const mongoose = require('mongoose');

const favoriteSchema = new mongoose.Schema({
    userId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    roomId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Room',
        required: true
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

// Ensure one favorite per user-room combination
favoriteSchema.index({ userId: 1, roomId: 1 }, { unique: true });

module.exports = mongoose.model('Favorite', favoriteSchema);


