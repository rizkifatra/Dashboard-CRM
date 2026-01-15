"use strict";
var __esDecorate = (this && this.__esDecorate) || function (ctor, descriptorIn, decorators, contextIn, initializers, extraInitializers) {
    function accept(f) { if (f !== void 0 && typeof f !== "function") throw new TypeError("Function expected"); return f; }
    var kind = contextIn.kind, key = kind === "getter" ? "get" : kind === "setter" ? "set" : "value";
    var target = !descriptorIn && ctor ? contextIn["static"] ? ctor : ctor.prototype : null;
    var descriptor = descriptorIn || (target ? Object.getOwnPropertyDescriptor(target, contextIn.name) : {});
    var _, done = false;
    for (var i = decorators.length - 1; i >= 0; i--) {
        var context = {};
        for (var p in contextIn) context[p] = p === "access" ? {} : contextIn[p];
        for (var p in contextIn.access) context.access[p] = contextIn.access[p];
        context.addInitializer = function (f) { if (done) throw new TypeError("Cannot add initializers after decoration has completed"); extraInitializers.push(accept(f || null)); };
        var result = (0, decorators[i])(kind === "accessor" ? { get: descriptor.get, set: descriptor.set } : descriptor[key], context);
        if (kind === "accessor") {
            if (result === void 0) continue;
            if (result === null || typeof result !== "object") throw new TypeError("Object expected");
            if (_ = accept(result.get)) descriptor.get = _;
            if (_ = accept(result.set)) descriptor.set = _;
            if (_ = accept(result.init)) initializers.unshift(_);
        }
        else if (_ = accept(result)) {
            if (kind === "field") initializers.unshift(_);
            else descriptor[key] = _;
        }
    }
    if (target) Object.defineProperty(target, contextIn.name, descriptor);
    done = true;
};
var __runInitializers = (this && this.__runInitializers) || function (thisArg, initializers, value) {
    var useValue = arguments.length > 2;
    for (var i = 0; i < initializers.length; i++) {
        value = useValue ? initializers[i].call(thisArg, value) : initializers[i].call(thisArg);
    }
    return useValue ? value : void 0;
};
var __setFunctionName = (this && this.__setFunctionName) || function (f, name, prefix) {
    if (typeof name === "symbol") name = name.description ? "[".concat(name.description, "]") : "";
    return Object.defineProperty(f, "name", { configurable: true, value: prefix ? "".concat(prefix, " ", name) : name });
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.DateUtilsService = void 0;
var core_1 = require("@angular/core");
var DateUtilsService = function () {
    var _classDecorators = [(0, core_1.Injectable)({
            providedIn: 'root',
        })];
    var _classDescriptor;
    var _classExtraInitializers = [];
    var _classThis;
    var DateUtilsService = _classThis = /** @class */ (function () {
        function DateUtilsService_1() {
        }
        /**
         * Get the first day of the current month
         */
        DateUtilsService_1.prototype.getCurrentMonthStart = function () {
            var today = new Date();
            var firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
            return this.formatDateToISO(firstDay);
        };
        /**
         * Get the last day of the current month
         */
        DateUtilsService_1.prototype.getCurrentMonthEnd = function () {
            var today = new Date();
            var lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
            return this.formatDateToISO(lastDay);
        };
        /**
         * Get the date for N days ago
         * @param days Number of days in the past
         */
        DateUtilsService_1.prototype.getDaysAgo = function (days) {
            var date = new Date();
            date.setDate(date.getDate() - days);
            return this.formatDateToISO(date);
        };
        /**
         * Format a Date object to ISO date string (YYYY-MM-DD)
         * @param date The date to format
         */
        DateUtilsService_1.prototype.formatDateToISO = function (date) {
            return date.toISOString().split('T')[0];
        };
        /**
         * Format date string to readable format
         * @param dateString ISO date string
         * @param options Intl.DateTimeFormatOptions
         */
        DateUtilsService_1.prototype.formatDate = function (dateString, options) {
            if (options === void 0) { options = {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
            }; }
            if (!dateString)
                return 'N/A';
            var date = new Date(dateString);
            return date.toLocaleDateString('en-US', options);
        };
        /**
         * Get the month name
         * @param monthIndex Month index (0-11)
         */
        DateUtilsService_1.prototype.getMonthName = function (monthIndex) {
            var months = [
                'January',
                'February',
                'March',
                'April',
                'May',
                'June',
                'July',
                'August',
                'September',
                'October',
                'November',
                'December',
            ];
            return months[monthIndex];
        };
        /**
         * Get current month and year as string
         */
        DateUtilsService_1.prototype.getCurrentMonthYear = function () {
            var today = new Date();
            return "".concat(this.getMonthName(today.getMonth()), " ").concat(today.getFullYear());
        };
        /**
         * Get date range for last 7 days
         */
        DateUtilsService_1.prototype.getLast7Days = function () {
            var today = new Date();
            var from = new Date();
            from.setDate(today.getDate() - 7);
            return {
                from: this.formatDateToISO(from),
                to: this.formatDateToISO(today),
            };
        };
        /**
         * Get date range for last 30 days
         */
        DateUtilsService_1.prototype.getLast30Days = function () {
            var today = new Date();
            var from = new Date();
            from.setDate(today.getDate() - 30);
            return {
                from: this.formatDateToISO(from),
                to: this.formatDateToISO(today),
            };
        };
        /**
         * Get date range for last 90 days
         */
        DateUtilsService_1.prototype.getLast90Days = function () {
            var today = new Date();
            var from = new Date();
            from.setDate(today.getDate() - 90);
            return {
                from: this.formatDateToISO(from),
                to: this.formatDateToISO(today),
            };
        };
        /**
         * Get date range for last 6 months
         */
        DateUtilsService_1.prototype.getLast6Months = function () {
            var today = new Date();
            var from = new Date();
            from.setMonth(today.getMonth() - 6);
            return {
                from: this.formatDateToISO(from),
                to: this.formatDateToISO(today),
            };
        };
        /**
         * Get date range for last year
         */
        DateUtilsService_1.prototype.getLastYear = function () {
            var today = new Date();
            var from = new Date();
            from.setFullYear(today.getFullYear() - 1);
            return {
                from: this.formatDateToISO(from),
                to: this.formatDateToISO(today),
            };
        };
        /**
         * Get date range for this year
         */
        DateUtilsService_1.prototype.getThisYear = function () {
            var today = new Date();
            var from = new Date(today.getFullYear(), 0, 1);
            return {
                from: this.formatDateToISO(from),
                to: this.formatDateToISO(today),
            };
        };
        /**
         * Get date range for this quarter
         */
        DateUtilsService_1.prototype.getThisQuarter = function () {
            var today = new Date();
            var quarter = Math.floor(today.getMonth() / 3);
            var from = new Date(today.getFullYear(), quarter * 3, 1);
            return {
                from: this.formatDateToISO(from),
                to: this.formatDateToISO(today),
            };
        };
        /**
         * Get date range for last quarter
         */
        DateUtilsService_1.prototype.getLastQuarter = function () {
            var today = new Date();
            var currentQuarter = Math.floor(today.getMonth() / 3);
            var lastQuarter = currentQuarter === 0 ? 3 : currentQuarter - 1;
            var year = currentQuarter === 0 ? today.getFullYear() - 1 : today.getFullYear();
            var from = new Date(year, lastQuarter * 3, 1);
            var to = new Date(year, lastQuarter * 3 + 3, 0);
            return {
                from: this.formatDateToISO(from),
                to: this.formatDateToISO(to),
            };
        };
        /**
         * Get all time (no date filter - returns empty strings)
         */
        DateUtilsService_1.prototype.getAllTime = function () {
            return {
                from: '',
                to: '',
            };
        };
        /**
         * Get last 12 months with labels
         */
        DateUtilsService_1.prototype.getLast12Months = function () {
            var result = [];
            var today = new Date();
            for (var i = 0; i < 12; i++) {
                var date = new Date(today.getFullYear(), today.getMonth() - i, 1);
                var monthName = this.getMonthName(date.getMonth());
                var year = date.getFullYear();
                result.push({
                    label: "".concat(monthName, " ").concat(year),
                    year: year,
                    month: date.getMonth() + 1,
                });
            }
            return result;
        };
        return DateUtilsService_1;
    }());
    __setFunctionName(_classThis, "DateUtilsService");
    (function () {
        var _metadata = typeof Symbol === "function" && Symbol.metadata ? Object.create(null) : void 0;
        __esDecorate(null, _classDescriptor = { value: _classThis }, _classDecorators, { kind: "class", name: _classThis.name, metadata: _metadata }, null, _classExtraInitializers);
        DateUtilsService = _classThis = _classDescriptor.value;
        if (_metadata) Object.defineProperty(_classThis, Symbol.metadata, { enumerable: true, configurable: true, writable: true, value: _metadata });
        __runInitializers(_classThis, _classExtraInitializers);
    })();
    return DateUtilsService = _classThis;
}();
exports.DateUtilsService = DateUtilsService;
