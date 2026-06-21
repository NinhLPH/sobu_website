import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { CustomerService } from '../service/custom.service';
import { RequestResponseDto } from '../interface/customer-request.model';
import { useRequestStore } from './useRequestStore';

jest.mock('../service/custom.service');

const mockedCustomerService = jest.mocked(CustomerService);

const requestDetail: RequestResponseDto = {
    id: 123,
    requestCode: 'REQ-123',
    customerPhone: '0901234567',
    type: 'CUSTOM',
    status: 'REVIEWING',
    totalAmount: 1000000,
    depositAmount: 0,
    customRequirements: 'Current request',
    items: [
        {
            id: 1,
            name: 'Current item',
            price: 1000000,
            quantity: 1
        }
    ],
    attachments: [],
    createdAt: '2026-06-15T00:00:00Z',
    updatedAt: '2026-06-15T00:00:00Z'
};

describe('useRequestStore conflicts', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useRequestStore.setState({
            currentRequestDetail: requestDetail,
            myRequests: [requestDetail],
            adminRequests: [],
            isSubmitting: false,
            error: null
        });
    });

    it('keeps current data and exposes a reload message on 409', async () => {
        mockedCustomerService.updateRequest.mockRejectedValue({
            response: {
                status: 409,
                data: {
                    message: 'Backend conflict'
                }
            }
        });

        await expect(useRequestStore.getState().updateRequestAction(123, {
            customRequirements: 'Changed request'
        })).rejects.toThrow(
            'Yêu cầu đã thay đổi hoặc không còn được phép cập nhật. Vui lòng tải lại dữ liệu mới nhất.'
        );

        const state = useRequestStore.getState();
        expect(state.currentRequestDetail).toBe(requestDetail);
        expect(state.myRequests).toEqual([requestDetail]);
        expect(state.isSubmitting).toBe(false);
        expect(state.error).toContain('Vui lòng tải lại dữ liệu mới nhất');
    });
});
