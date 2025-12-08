const mongoose = require('mongoose');
const Room = require('../models/Room');
require('dotenv').config();

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/homestay';

async function updateRoomRatings() {
    try {
        // Connect to MongoDB
        await mongoose.connect(MONGODB_URI);
        console.log('✅ Connected to MongoDB');

        // Update all rooms to have rating = 5.0 and reviewCount = 10
        const result = await Room.updateMany(
            {}, // Update all rooms
            {
                $set: {
                    rating: 5.0,
                    reviewCount: 10
                }
            }
        );

        console.log(`✅ Updated ${result.modifiedCount} rooms with rating = 5.0 and reviewCount = 10`);

        // Display updated rooms
        const rooms = await Room.find({}, 'name rating reviewCount');
        console.log('\n📋 Updated rooms:');
        rooms.forEach(room => {
            console.log(`  - ${room.name}: ${room.rating} (${room.reviewCount} reviews)`);
        });

        await mongoose.connection.close();
        console.log('\n✅ Done!');
    } catch (error) {
        console.error('❌ Error updating room ratings:', error);
        process.exit(1);
    }
}

updateRoomRatings();











