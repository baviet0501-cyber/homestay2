require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { connectDB } = require('./database/db');
const { seedDatabase } = require('./data/seed');
const { seedAdmin } = require('./data/seedAdmin');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors()); // Cho phép tất cả origins (trong production nên giới hạn)
app.use(express.json()); // Parse JSON body

// Security middleware
const { rateLimiters } = require('./utils/security');

// Routes
// Áp dụng rate limiter chỉ cho login, không áp dụng cho register
const authRouter = require('./routes/auth');
app.use('/api/auth/login', rateLimiters.auth, authRouter); // Rate limiter cho login
app.use('/api/auth', authRouter); // Các route khác (register, etc.) không có rate limiter
app.use('/api/users', require('./routes/users'));
app.use('/api/rooms', require('./routes/rooms'));
app.use('/api/bookings', require('./routes/bookings'));
app.use('/api/favorites', require('./routes/favorites'));
app.use('/api/admin', require('./routes/admin')); // Admin routes

// Health check
app.get('/health', (req, res) => {
    res.json({ status: 'OK', message: 'Server is running' });
});

// Root endpoint
app.get('/', (req, res) => {
    res.json({
        message: 'Homestay Backend API',
        version: '1.0.0',
        database: 'MongoDB',
        endpoints: {
            auth: '/api/auth',
            users: '/api/users',
            rooms: '/api/rooms',
            bookings: '/api/bookings',
            favorites: '/api/favorites'
        }
    });
});

// Connect to MongoDB and start server
async function startServer() {
    try {
        await connectDB();
        await seedDatabase(); // Seed sample data
        await seedAdmin(); // Seed admin account
        
        app.listen(PORT, '0.0.0.0', () => {
            console.log(`🚀 Server is running on http://localhost:${PORT}`);
            console.log(`📡 Listening on all network interfaces (0.0.0.0:${PORT})`);
            console.log(`📱 Android app should connect to: http://[YOUR_IP]:${PORT}/api/`);
            console.log(`🗄️  Database: MongoDB`);
        });
    } catch (error) {
        console.error('❌ Failed to start server:', error);
        process.exit(1);
    }
}

startServer();

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('\n🛑 Shutting down server...');
    const mongoose = require('mongoose');
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
    process.exit(0);
});

