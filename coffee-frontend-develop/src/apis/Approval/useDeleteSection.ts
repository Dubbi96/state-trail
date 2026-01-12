import { useMutation, useQueryClient } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { showToast } from '@/utils/showToast';

import { axiosInstance } from '../axiosInstance';

type DeleteSectionReq = {
    sectionId: number;
};

const deleteSection = async ({ sectionId }: DeleteSectionReq) => {
    const response = await axiosInstance.delete(`/approval/section/${sectionId}`, {
        params: { approverId: 1 },
    });
    return response.data;
};

export const useDeleteSection = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteSection,
        onSuccess: (res) => {
            if (res.code === 'SUCCESS') {
                queryClient.invalidateQueries({
                    queryKey: QUERY_KEYS.AREA.getAreaWithSectionList(),
                });
                showToast.success('삭제 요청을 성공했습니다.');
                return;
            }
            showToast.error('삭제 요청을 실패해습니다.');
            return;
        },
        onError: (res) => {
            showToast.error('삭제 요청을 실패해습니다.' + `\n${res.message ?? ''}`);
        },
    });
};
