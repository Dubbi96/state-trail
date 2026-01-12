import { useMutation, useQueryClient } from '@tanstack/react-query';

import { axiosInstance } from '../axiosInstance';

type DeleteApprovalReq = {
    approvalId: number;
};

const deleteApproval = async ({ approvalId }: DeleteApprovalReq) => {
    const response = await axiosInstance.delete(`/approval/${approvalId}`);
    return response.data;
};

export const useDeleteApproval = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteApproval,
        onSuccess: (_data) => {
            queryClient.invalidateQueries({
                queryKey: ['approval'],
            });
        },
    });
};
