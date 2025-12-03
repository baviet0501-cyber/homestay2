/**
 * Input Validator - Xác thực và làm sạch đầu vào toàn diện
 * Ngăn chặn các cuộc tấn công injection, XSS và xác thực kiểu dữ liệu
 */

const { sanitizeInput } = require('../validators');

class InputValidator {
    /**
     * Xác thực và làm sạch đầu vào chuỗi
     * @param {string} value - Giá trị cần xác thực
     * @param {Object} options - Tùy chọn xác thực
     * @returns {Object} - { isValid: boolean, value: string, error: string }
     */
    static validateString(value, options = {}) {
        const {
            required = false,
            minLength = 0,
            maxLength = Infinity,
            pattern = null,
            trim = true,
            sanitize = true
        } = options;

        // Kiểm tra bắt buộc
        if (required && (!value || (typeof value === 'string' && value.trim().length === 0))) {
            return {
                isValid: false,
                value: null,
                error: 'Trường này là bắt buộc'
            };
        }

        // Nếu không bắt buộc và rỗng, trả về hợp lệ
        if (!value || (typeof value === 'string' && value.trim().length === 0)) {
            return {
                isValid: true,
                value: '',
                error: null
            };
        }

        // Kiểm tra kiểu dữ liệu
        if (typeof value !== 'string') {
            return {
                isValid: false,
                value: null,
                error: 'Kiểu dữ liệu không hợp lệ. Mong đợi chuỗi.'
            };
        }

        // Xử lý giá trị
        let processedValue = value;
        if (trim) {
            processedValue = processedValue.trim();
        }
        if (sanitize) {
            processedValue = sanitizeInput(processedValue);
        }

        // Xác thực độ dài
        if (processedValue.length < minLength) {
            return {
                isValid: false,
                value: null,
                error: `Độ dài tối thiểu là ${minLength} ký tự`
            };
        }

        if (processedValue.length > maxLength) {
            return {
                isValid: false,
                value: null,
                error: `Độ dài tối đa là ${maxLength} ký tự`
            };
        }

        // Xác thực mẫu
        if (pattern && !pattern.test(processedValue)) {
            return {
                isValid: false,
                value: null,
                error: 'Định dạng không hợp lệ'
            };
        }

        return {
            isValid: true,
            value: processedValue,
            error: null
        };
    }

    /**
     * Xác thực email
     * @param {string} email - Email cần xác thực
     * @returns {Object} - { isValid: boolean, value: string, error: string }
     */
    static validateEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return this.validateString(email, {
            required: true,
            maxLength: 255,
            pattern: emailRegex,
            sanitize: true
        });
    }

    /**
     * Xác thực số điện thoại
     * @param {string} phone - Số điện thoại cần xác thực
     * @returns {Object} - { isValid: boolean, value: string, error: string }
     */
    static validatePhone(phone) {
        const phoneRegex = /^0\d{9,10}$/;
        const digitsOnly = phone ? phone.replace(/\D/g, '') : '';

        return this.validateString(digitsOnly, {
            required: true,
            minLength: 10,
            maxLength: 11,
            pattern: phoneRegex,
            sanitize: false
        });
    }

    /**
     * Xác thực số
     * @param {any} value - Giá trị cần xác thực
     * @param {Object} options - Tùy chọn xác thực
     * @returns {Object} - { isValid: boolean, value: number, error: string }
     */
    static validateNumber(value, options = {}) {
        const {
            required = false,
            min = -Infinity,
            max = Infinity,
            integer = false
        } = options;

        if (required && (value === null || value === undefined || value === '')) {
            return {
                isValid: false,
                value: null,
                error: 'Trường này là bắt buộc'
            };
        }

        if (!required && (value === null || value === undefined || value === '')) {
            return {
                isValid: true,
                value: null,
                error: null
            };
        }

        const num = Number(value);
        if (isNaN(num)) {
            return {
                isValid: false,
                value: null,
                error: 'Số không hợp lệ'
            };
        }

        if (integer && !Number.isInteger(num)) {
            return {
                isValid: false,
                value: null,
                error: 'Phải là số nguyên'
            };
        }

        if (num < min) {
            return {
                isValid: false,
                value: null,
                error: `Giá trị phải ít nhất là ${min}`
            };
        }

        if (num > max) {
            return {
                isValid: false,
                value: null,
                error: `Giá trị tối đa là ${max}`
            };
        }

        return {
            isValid: true,
            value: integer ? Math.floor(num) : num,
            error: null
        };
    }

    /**
     * Xác thực ngày tháng
     * @param {any} value - Giá trị cần xác thực
     * @param {Object} options - Tùy chọn xác thực
     * @returns {Object} - { isValid: boolean, value: Date, error: string }
     */
    static validateDate(value, options = {}) {
        const {
            required = false,
            minDate = null,
            maxDate = null
        } = options;

        if (required && !value) {
            return {
                isValid: false,
                value: null,
                error: 'Trường này là bắt buộc'
            };
        }

        if (!required && !value) {
            return {
                isValid: true,
                value: null,
                error: null
            };
        }

        const date = new Date(value);
        if (isNaN(date.getTime())) {
            return {
                isValid: false,
                value: null,
                error: 'Định dạng ngày không hợp lệ'
            };
        }

        if (minDate && date < new Date(minDate)) {
            return {
                isValid: false,
                value: null,
                error: `Ngày phải sau ${new Date(minDate).toISOString()}`
            };
        }

        if (maxDate && date > new Date(maxDate)) {
            return {
                isValid: false,
                value: null,
                error: `Ngày phải trước ${new Date(maxDate).toISOString()}`
            };
        }

        return {
            isValid: true,
            value: date,
            error: null
        };
    }

    /**
     * Xác thực đối tượng với schema
     * @param {Object} data - Dữ liệu cần xác thực
     * @param {Object} schema - Schema xác thực
     * @returns {Object} - { isValid: boolean, data: Object, errors: Object }
     */
    static validateObject(data, schema) {
        const errors = {};
        const validatedData = {};

        for (const [key, rules] of Object.entries(schema)) {
            const value = data[key];
            let result;

            switch (rules.type) {
                case 'string':
                    result = this.validateString(value, rules);
                    break;
                case 'email':
                    result = this.validateEmail(value);
                    break;
                case 'phone':
                    result = this.validatePhone(value);
                    break;
                case 'number':
                    result = this.validateNumber(value, rules);
                    break;
                case 'date':
                    result = this.validateDate(value, rules);
                    break;
                default:
                    result = { isValid: true, value, error: null };
            }

            if (!result.isValid) {
                errors[key] = result.error;
            } else {
                validatedData[key] = result.value;
            }
        }

        return {
            isValid: Object.keys(errors).length === 0,
            data: validatedData,
            errors
        };
    }

    /**
     * Làm sạch đối tượng đệ quy
     * @param {any} obj - Đối tượng cần làm sạch
     * @returns {any} - Đối tượng đã làm sạch
     */
    static sanitizeObject(obj) {
        if (typeof obj === 'string') {
            return sanitizeInput(obj);
        }

        if (Array.isArray(obj)) {
            return obj.map(item => this.sanitizeObject(item));
        }

        if (obj && typeof obj === 'object') {
            const sanitized = {};
            for (const [key, value] of Object.entries(obj)) {
                sanitized[key] = this.sanitizeObject(value);
            }
            return sanitized;
        }

        return obj;
    }

    /**
     * Kiểm tra các mẫu SQL injection
     * @param {string} input - Đầu vào cần kiểm tra
     * @returns {boolean} - True nếu có khả năng nguy hiểm
     */
    static hasSQLInjection(input) {
        if (typeof input !== 'string') {
            return false;
        }

        const sqlPatterns = [
            /(\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE)\b)/i,
            /(--|#|\/\*|\*\/|;)/,
            /(\b(OR|AND)\s+\d+\s*=\s*\d+)/i,
            /(\bUNION\b.*\bSELECT\b)/i
        ];

        return sqlPatterns.some(pattern => pattern.test(input));
    }

    /**
     * Kiểm tra các mẫu XSS
     * @param {string} input - Đầu vào cần kiểm tra
     * @returns {boolean} - True nếu có khả năng nguy hiểm
     */
    static hasXSS(input) {
        if (typeof input !== 'string') {
            return false;
        }

        const xssPatterns = [
            /<script[^>]*>.*?<\/script>/gi,
            /<iframe[^>]*>.*?<\/iframe>/gi,
            /javascript:/gi,
            /on\w+\s*=/gi,
            /<img[^>]+src[^>]*=.*javascript:/gi
        ];

        return xssPatterns.some(pattern => pattern.test(input));
    }
}

module.exports = InputValidator;
