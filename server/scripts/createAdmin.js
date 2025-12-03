const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

// Admin Model
const adminSchema = new mongoose.Schema({
    username: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    fullName: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    role: { type: String, enum: ['admin', 'super_admin'], default: 'admin' },
    isActive: { type: Boolean, default: true },
    createdAt: { type: Date, default: Date.now }
});

const Admin = mongoose.model('Admin', adminSchema);

async function createAdmin() {
    try {
        // Connect to MongoDB
        const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/homestay';
        await mongoose.connect(MONGODB_URI);
        console.log('✅ Connected to MongoDB');

        // Check if admin exists
        const existingAdmin = await Admin.findOne({ username: 'admin' });
        if (existingAdmin) {
            console.log('⚠️  Admin account already exists');
            console.log('   Username: admin');
            console.log('   To reset password, delete and recreate');
            
            // Option: Update password
            const newPassword = await bcrypt.hash('Admin@123', 10);
            existingAdmin.password = newPassword;
            await existingAdmin.save();
            console.log('✅ Password reset to: Admin@123');
            
            await mongoose.connection.close();
            return;
        }

        // Create admin
        const hashedPassword = await bcrypt.hash('Admin@123', 10);
        const admin = new Admin({
            username: 'admin',
            password: hashedPassword,
            fullName: 'Administrator',
            email: 'admin@homeviet.com',
            role: 'super_admin',
            isActive: true
        });

        await admin.save();
        console.log('✅ Admin account created successfully!');
        console.log('   Username: admin');
        console.log('   Password: Admin@123');
        console.log('   Email: admin@homeviet.com');
        console.log('   ⚠️  CHANGE PASSWORD AFTER FIRST LOGIN!');

        await mongoose.connection.close();
    } catch (error) {
        console.error('❌ Error:', error);
        process.exit(1);
    }
}

createAdmin();

