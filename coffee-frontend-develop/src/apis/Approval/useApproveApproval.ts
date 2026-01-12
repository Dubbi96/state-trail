import { useMutation, useQueryClient } from '@tanstack/react-query';

import { axiosInstance } from '../axiosInstance';

const approveApproval = async (approvalId: string) => {
    const response = await axiosInstance.patch(`/approval/approve/${approvalId}`);
    return response.data;
};

export const useApproveApproval = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: approveApproval,
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({
                queryKey: ['approval', variables],
            });
        },
    });
};
