import { describe, expect, it, jest } from '@jest/globals';
import { AdminWorkflowService } from './admin.service';

const mockApiGet = jest.fn();
const mockApiPatch = jest.fn();

jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {
        get: (...args: any[]) => mockApiGet(...args),
        patch: (...args: any[]) => mockApiPatch(...args)
    }
}));

describe('AdminWorkflowService payment history', () => {
    it('loads persisted payments from the staff endpoint', () => {
        AdminWorkflowService.getAdminOrderPayments(12);

        expect(mockApiGet).toHaveBeenCalledWith('/v1/api/admin/payments/orders/12');
    });

    it('sends the backend status DTO to the admin order endpoint', () => {
        AdminWorkflowService.updateAdminOrderStatus(12, {status: 'PROCESSING'});

        expect(mockApiPatch).toHaveBeenCalledWith('/api/admin/orders/12/status', {
            status: 'PROCESSING'
        });
    });
});
