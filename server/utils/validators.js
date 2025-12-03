/**
 * Tiện ích xác thực cho validation đầu vào
 */

/**
 * Xác thực định dạng email
 * @param {string} email - Email cần xác thực
 * @returns {boolean} - True nếu hợp lệ
 */
function validateEmail(email) {
    if (!email || typeof email !== 'string') {
        return false;
    }

    // Mẫu regex email cơ bản
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email.trim());
}

/**
 * Xác thực số điện thoại Việt Nam
 * @param {string} phone - Số điện thoại cần xác thực
 * @returns {boolean} - True nếu hợp lệ
 */
function validatePhoneNumber(phone) {
    if (!phone || typeof phone !== 'string') {
        return false;
    }

    // Xóa tất cả ký tự không phải số
    const digitsOnly = phone.replace(/\D/g, '');

    // Số điện thoại Việt Nam: 10-11 chữ số, bắt đầu bằng 0
    // Định dạng: 0xxxxxxxxx (10 chữ số) hoặc 0xxxxxxxxxx (11 chữ số)
    const phoneRegex = /^0\d{9,10}$/;
    return phoneRegex.test(digitsOnly);
}

/**
 * Chuẩn hóa số điện thoại về định dạng tiêu chuẩn
 * @param {string} phone - Số điện thoại cần chuẩn hóa
 * @returns {string} - Số điện thoại đã chuẩn hóa
 */
function normalizePhoneNumber(phone) {
    if (!phone || typeof phone !== 'string') {
        return '';
    }

    // Xóa tất cả ký tự không phải số
    return phone.replace(/\D/g, '');
}

/**
 * Xác thực độ mạnh mật khẩu
 * @param {string} password - Mật khẩu cần xác thực
 * @returns {Object} - {isValid: boolean, message: string}
 */
function validatePassword(password) {
    if (!password || typeof password !== 'string') {
        return {
            isValid: false,
            message: 'Mật khẩu không được để trống'
        };
    }

    if (password.length < 6) {
        return {
            isValid: false,
            message: 'Mật khẩu phải có ít nhất 6 ký tự'
        };
    }

    if (password.length > 50) {
        return {
            isValid: false,
            message: 'Mật khẩu không được vượt quá 50 ký tự'
        };
    }

    return {
        isValid: true,
        message: 'Mật khẩu hợp lệ'
    };
}

/**
 * Xác thực họ và tên
 * @param {string} fullName - Họ và tên cần xác thực
 * @returns {boolean} - True nếu hợp lệ
 */
function validateFullName(fullName) {
    if (!fullName || typeof fullName !== 'string') {
        return false;
    }

    const trimmed = fullName.trim();

    // Họ và tên phải có từ 2 đến 50 ký tự
    if (trimmed.length < 2 || trimmed.length > 50) {
        return false;
    }

    // Chỉ chứa chữ cái, khoảng trắng và ký tự tiếng Việt
    const nameRegex = /^[a-zA-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂưăạảấầẩẫậắằẳẵặẹẻẽềềểỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪễệỉịọỏốồổỗộớờởỡợụủứừỬỮỰỲỴÝỶỸửữựỳỵỷỹ\s]+$/;
    return nameRegex.test(trimmed);
}

/**
 * Làm sạch đầu vào để ngăn chặn tấn công XSS
 * @param {string} input - Đầu vào cần làm sạch
 * @returns {string} - Đầu vào đã làm sạch
 */
function sanitizeInput(input) {
    if (!input || typeof input !== 'string') {
        return '';
    }

    // Xóa thẻ HTML và cắt khoảng trắng
    return input
        .replace(/<[^>]*>/g, '') // Xóa thẻ HTML
        .trim()
        .replace(/\s+/g, ' '); // Chuẩn hóa khoảng trắng
}

module.exports = {
    validateEmail,
    validatePhoneNumber,
    normalizePhoneNumber,
    validatePassword,
    validateFullName,
    sanitizeInput
};
