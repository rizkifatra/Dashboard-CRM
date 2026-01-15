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
exports.OpportunityService = void 0;
var core_1 = require("@angular/core");
var http_1 = require("@angular/common/http");
var OpportunityService = function () {
    var _classDecorators = [(0, core_1.Injectable)({
            providedIn: 'root',
        })];
    var _classDescriptor;
    var _classExtraInitializers = [];
    var _classThis;
    var OpportunityService = _classThis = /** @class */ (function () {
        function OpportunityService_1(http) {
            this.http = http;
            this.apiUrl = 'http://localhost:8080/api/opportunities';
        }
        /**
         * Get all opportunities with pagination support
         * @param skip Number of records to skip (for pagination)
         * @param top Maximum number of records to return (default: 50)
         * @param search Search term for filtering opportunities
         * @param fromDate Optional start date filter
         * @param toDate Optional end date filter
         */
        OpportunityService_1.prototype.getAllOpportunities = function (skip, top, search, fromDate, toDate) {
            if (skip === void 0) { skip = 0; }
            if (top === void 0) { top = 50; }
            if (search === void 0) { search = ''; }
            var params = new http_1.HttpParams()
                .set('top', top.toString())
                .set('skip', skip.toString());
            if (search)
                params = params.set('search', search);
            if (fromDate)
                params = params.set('fromDate', fromDate);
            if (toDate)
                params = params.set('toDate', toDate);
            return this.http.get("".concat(this.apiUrl, "/all"), {
                params: params,
            });
        };
        /**
         * Get opportunity statistics
         */
        OpportunityService_1.prototype.getOpportunityStatistics = function (fromDate, toDate) {
            var params = new http_1.HttpParams();
            if (fromDate)
                params = params.set('fromDate', fromDate);
            if (toDate)
                params = params.set('toDate', toDate);
            return this.http.get("".concat(this.apiUrl, "/stats"), { params: params });
        };
        /**
         * Get opportunities by staff
         */
        OpportunityService_1.prototype.getOpportunitiesByStaff = function (fromDate, toDate) {
            var params = new http_1.HttpParams();
            if (fromDate)
                params = params.set('fromDate', fromDate);
            if (toDate)
                params = params.set('toDate', toDate);
            return this.http.get("".concat(this.apiUrl, "/by-staff"), { params: params });
        };
        /**
         * Get top opportunities by value
         */
        OpportunityService_1.prototype.getTopOpportunities = function (top, fromDate, toDate) {
            if (top === void 0) { top = 10; }
            var params = new http_1.HttpParams().set('top', top.toString());
            if (fromDate)
                params = params.set('fromDate', fromDate);
            if (toDate)
                params = params.set('toDate', toDate);
            return this.http.get("".concat(this.apiUrl, "/top"), {
                params: params,
            });
        };
        /**
         * Get opportunity by ID
         */
        OpportunityService_1.prototype.getOpportunityById = function (id) {
            return this.http.get("".concat(this.apiUrl, "/").concat(id));
        };
        /**
         * Get count of active opportunities
         */
        OpportunityService_1.prototype.getActiveOpportunityCount = function (fromDate, toDate) {
            var params = new http_1.HttpParams();
            if (fromDate)
                params = params.set('fromDate', fromDate);
            if (toDate)
                params = params.set('toDate', toDate);
            return this.http.get("".concat(this.apiUrl, "/count"), {
                params: params,
            });
        };
        /**
         * Format currency value
         */
        OpportunityService_1.prototype.formatCurrency = function (value) {
            if (!value)
                return 'RM 0';
            return "RM ".concat(value.toLocaleString('en-MY', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 0,
            }));
        };
        /**
         * Format percentage
         */
        OpportunityService_1.prototype.formatPercentage = function (value) {
            return "".concat(value.toFixed(1), "%");
        };
        /**
         * Get status label
         */
        OpportunityService_1.prototype.getStatusLabel = function (stateCode) {
            switch (stateCode) {
                case 0:
                    return 'Open';
                case 1:
                    return 'Won';
                case 2:
                    return 'Lost';
                default:
                    return 'Unknown';
            }
        };
        /**
         * Get status color class
         */
        OpportunityService_1.prototype.getStatusColorClass = function (stateCode) {
            switch (stateCode) {
                case 0:
                    return 'status-open';
                case 1:
                    return 'status-won';
                case 2:
                    return 'status-lost';
                default:
                    return '';
            }
        };
        /**
         * Get monthly trends
         */
        OpportunityService_1.prototype.getMonthlyTrends = function (months) {
            if (months === void 0) { months = 6; }
            var params = new http_1.HttpParams().set('months', months.toString());
            return this.http.get("".concat(this.apiUrl, "/monthly-trends"), {
                params: params,
            });
        };
        return OpportunityService_1;
    }());
    __setFunctionName(_classThis, "OpportunityService");
    (function () {
        var _metadata = typeof Symbol === "function" && Symbol.metadata ? Object.create(null) : void 0;
        __esDecorate(null, _classDescriptor = { value: _classThis }, _classDecorators, { kind: "class", name: _classThis.name, metadata: _metadata }, null, _classExtraInitializers);
        OpportunityService = _classThis = _classDescriptor.value;
        if (_metadata) Object.defineProperty(_classThis, Symbol.metadata, { enumerable: true, configurable: true, writable: true, value: _metadata });
        __runInitializers(_classThis, _classExtraInitializers);
    })();
    return OpportunityService = _classThis;
}();
exports.OpportunityService = OpportunityService;
