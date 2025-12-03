require('dotenv').config();
const mongoose = require('mongoose');
const Room = require('../models/Room');
const Slot = require('../models/Slot');

// Rooms data từ DatabaseSeeder.kt
const rooms = [
    {
        name: 'Sofitel Legend Metropole Hanoi',
        description: 'Khách sạn 5 sao cổ điển nổi tiếng tại trung tâm Hà Nội, có hồ bơi, spa và dịch vụ Butler.',
        price: 6500000,
        capacity: 2,
        imageUrl: 'https://lh3.googleusercontent.com/p/AF1QipMTu8qrDHddAz0TdAxDZwejYWjNIarfdyWiKMrr=s1360-w1360-h1020-rw',
        maxSlots: 1
    },
    {
        name: 'Capella Hanoi',
        description: 'Khách sạn 5 sao phong cách nghệ thuật với thiết kế Opera độc đáo và spa cao cấp.',
        price: 7200000,
        capacity: 2,
        imageUrl: 'https://lh3.googleusercontent.com/p/AF1QipMHw3eGpuKOXmgCPAAPwYqFDPx9vl1UpFTnP2Kl=s1360-w1360-h1020-rw?w=800',
        maxSlots: 1
    },
    {
        name: 'JW Marriott Hanoi',
        description: 'Khách sạn 5 sao hiện đại với hội trường lớn, thiết kế độc đáo và hồ bơi trong nhà.',
        price: 4800000,
        capacity: 3,
        imageUrl: 'https://dynamic-media-cdn.tripadvisor.com/media/photo-o/30/58/42/53/entrance.jpg?w=800',
        maxSlots: 2
    },
    {
        name: 'Park Hyatt Saigon',
        description: 'Khách sạn 5 sao sang trọng trung tâm Quận 1 với hồ bơi ngoài trời và dịch vụ đẳng cấp.',
        price: 6100000,
        capacity: 2,
        imageUrl: 'https://lh3.googleusercontent.com/p/AF1QipNsROoEsRmTrLt1MBN97_hZFlP0mZsGEJpJP1Mr=s1360-w1360-h1020-rw?w=800',
        maxSlots: 1
    },
    {
        name: 'The Reverie Saigon',
        description: 'Khách sạn siêu sang phong cách Ý, view toàn cảnh trung tâm thành phố.',
        price: 8900000,
        capacity: 2,
        imageUrl: 'https://cf.bstatic.com/xdata/images/hotel/max1024x768/261534199.jpg?k=a814e0ccf607d334377f8a6f2beba859066823e545c8004e61d5b0183a318287&o=',
        maxSlots: 1
    },
    {
        name: 'Caravelle Saigon',
        description: 'Khách sạn 5 sao lâu đời tại trung tâm Quận 1, có rooftop bar và hồ bơi.',
        price: 3900000,
        capacity: 2,
        imageUrl: 'https://lh3.googleusercontent.com/p/AF1QipON3b9pT__cTQ-i6XRQURnoiqZ8rdmHLnoUhMfk=s1360-w1360-h1020-rw',
        maxSlots: 1
    },
    {
        name: 'InterContinental Danang Sun Peninsula Resort',
        description: 'Resort 5 sao trên bán đảo Sơn Trà với bãi biển riêng, thiết kế bởi Bill Bensley.',
        price: 12000000,
        capacity: 3,
        imageUrl: 'https://duan-sungroup.com/wp-content/uploads/2022/12/intercontinental-da-nang-sun-peninsula-resort-leading.png',
        maxSlots: 2
    },
    {
        name: 'Four Seasons The Nam Hai',
        description: 'Resort 5 sao hàng đầu châu Á, villa hướng biển, dịch vụ chuẩn quốc tế.',
        price: 15000000,
        capacity: 4,
        imageUrl: 'https://lh3.googleusercontent.com/p/AF1QipPnrwRzjWMI7XhrIR50yMjensoJEQMIeO2pDW6-=s1360-w1360-h1020-rw',
        maxSlots: 3
    },
    {
        name: 'JW Marriott Phu Quoc Emerald Bay',
        description: 'Resort 5 sao thiết kế cổ điển, bãi biển riêng và hồ bơi lớn.',
        price: 6800000,
        capacity: 3,
        imageUrl: 'https://cf.bstatic.com/xdata/images/hotel/max1024x768/490645960.jpg?k=9972c3434e640814a45b0f5d5cb24b0bede60bc4bab2296e2754c55bf6863565&o=',
        maxSlots: 2
    },
    {
        name: 'Regent Phu Quoc',
        description: 'Resort siêu sang với hồ bơi vô cực, villa riêng và dịch vụ chuẩn 6 sao.',
        price: 19000000,
        capacity: 4,
        imageUrl: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800',
        maxSlots: 3
    },
    {
        name: 'Vinpearl Resort & Spa Nha Trang',
        description: 'Resort lớn trên đảo Hòn Tre, phù hợp gia đình, có công viên nước và bãi biển riêng.',
        price: 3500000,
        capacity: 4,
        imageUrl: 'https://lh3.googleusercontent.com/p/AF1QipMTahweerxB8okpWaaxbr11ze36zfSbl1cuRmjD=s1360-w1360-h1020-rw',
        maxSlots: 3
    },
    {
        name: 'Azerai La Residence Hue',
        description: 'Khách sạn boutique ven sông Hương phong cách Pháp cổ điển.',
        price: 4200000,
        capacity: 2,
        imageUrl: 'https://lh3.googleusercontent.com/p/AF1QipM6Qn9lurhbDj_MQVkIMorFkDoRPp7iQJI3AIyC=s1360-w1360-h1020-rw',
        maxSlots: 1
    },
    {
        name: 'Victoria Sapa Resort & Spa',
        description: 'Resort nghỉ dưỡng phong cách núi rừng, view thung lũng Mường Hoa.',
        price: 2400000,
        capacity: 3,
        imageUrl: 'https://lh3.googleusercontent.com/p/AF1QipM1A1PMD6bPjZIj_x7-ljrEiAATg8_bw2GU3PN4=s1360-w1360-h1020-rw',
        maxSlots: 2
    },
    {
        name: 'Ana Mandara Villas Dalat',
        description: 'Khu nghỉ dưỡng cổ điển kiểu Pháp giữa đồi thông, không gian yên tĩnh.',
        price: 2900000,
        capacity: 2,
        imageUrl: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800',
        maxSlots: 1
    },
    {
        name: 'The Grand Ho Tram Resort & Casino',
        description: 'Khu nghỉ dưỡng lớn ven biển Hồ Tràm, có casino, hồ bơi và sân golf.',
        price: 3200000,
        capacity: 3,
        imageUrl: 'https://ik.imagekit.io/tvlk/apr-asset/dgXfoyh24ryQLRcGq00cIdKHRmotrWLNlvG-TxlcLxGkiDwaUSggleJNPRgIHCX6/hotel/asset/10019095-74f2051801f2cbc07bf3166fd7b23f0e.jpeg?tr=q-40,w-740,h-465&_src=imagekit',
        maxSlots: 2
    }
];

async function reseedRooms() {
    try {
        // Connect to MongoDB
        const { connectDB } = require('../database/db');
        await connectDB();
        console.log('✅ Connected to MongoDB');

        // Delete all existing rooms and slots
        const deletedRooms = await Room.deleteMany({});
        console.log(`🗑️  Deleted ${deletedRooms.deletedCount} existing rooms`);

        // Insert new rooms
        const insertedRooms = await Room.insertMany(rooms);
        console.log(`✅ Inserted ${insertedRooms.length} new rooms`);

        // Sample slots for first room
        if (insertedRooms.length > 0) {
            // Delete all slots first
            await Slot.deleteMany({});
            console.log('🗑️  Deleted existing slots');

            const slots = [
                {
                    roomId: insertedRooms[0]._id,
                    name: 'Sáng (7:00 - 12:00)',
                    price: 400000,
                    startTime: '07:00',
                    endTime: '12:00'
                },
                {
                    roomId: insertedRooms[0]._id,
                    name: 'Chiều (12:00 - 18:00)',
                    price: 450000,
                    startTime: '12:00',
                    endTime: '18:00'
                },
                {
                    roomId: insertedRooms[0]._id,
                    name: 'Tối (18:00 - 23:00)',
                    price: 500000,
                    startTime: '18:00',
                    endTime: '23:00'
                }
            ];

            const insertedSlots = await Slot.insertMany(slots);
            console.log(`✅ Inserted ${insertedSlots.length} slots`);
        }

        console.log('\n✅ Reseed completed successfully!');
        console.log(`📦 Total rooms in database: ${insertedRooms.length}`);
        
        // Close connection
        await mongoose.connection.close();
        process.exit(0);
    } catch (error) {
        console.error('❌ Error reseeding rooms:', error);
        process.exit(1);
    }
}

// Run script
reseedRooms();

