import { useMutation, useQueryClient } from '@tanstack/react-query';

import { axiosInstance } from '../axiosInstance';

type RejectApprovalReq = {
    approvalId: string;
    rejectedReason: string;
};

const rejectApproval = async ({ approvalId, rejectedReason }: RejectApprovalReq) => {
    const response = await axiosInstance.patch(`/approval/reject/${approvalId}`, {
        rejectedReason,
    });
    return response.data;
};

export const useRejectApproval = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: rejectApproval,
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({
                queryKey: ['approval', variables.approvalId],
            });
        },
    });
};
