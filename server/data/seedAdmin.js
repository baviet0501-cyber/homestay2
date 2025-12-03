const bcrypt = require('bcryptjs');
const Admin = require('../models/Admin');

/**
 * Seed default admin account
 * Username: admin
 * Password: Admin@123
 */
async function seedAdmin() {
    try {
        // Check if admin already exists
        const existingAdmin = await Admin.findOne({ username: 'admin' });
        if (existingAdmin) {
            console.log('👤 Admin account already exists');
            return;
        }

        // Create default admin
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
        console.log('✅ Default admin account created:');
        console.log('   Username: admin');
        console.log('   Password: Admin@123');
        console.log('   ⚠️  CHANGE PASSWORD AFTER FIRST LOGIN!');
    } catch (error) {
        console.error('❌ Error seeding admin:', error);
    }
}

module.exports = { seedAdmin };

