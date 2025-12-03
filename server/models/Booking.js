const mongoose = require('mongoose');

const bookingSchema = new mongoose.Schema({
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
    checkInDate: {
        type: Date,
        required: true
    },
    checkOutDate: {
        type: Date,
        required: true
    },
    guestCount: {
        type: Number,
        required: true,
        min: 1
    },
    totalPrice: {
        type: Number,
        required: true,
        min: 0
    },
    status: {
        type: String,
        enum: ['pending', 'confirmed', 'cancelled', 'completed'],
        default: 'pending'
    },
    paymentMethod: {
        type: String,
        enum: ['qr_code', 'momo', 'zalopay', 'pay_on_site', null],
        default: null
    },
    slotId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Slot',
        default: null
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

// Index for efficient queries
bookingSchema.index({ userId: 1 });
bookingSchema.index({ roomId: 1 });
bookingSchema.index({ checkInDate: 1, checkOutDate: 1 });

module.exports = mongoose.model('Booking', bookingSchema);


