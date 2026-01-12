import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { showToast } from '@/utils/showToast';

import { axiosInstance } from '../axiosInstance';

type DeleteSectionReq = {
    sectionId: number;
};

const deleteSectionAdmin = async ({ sectionId }: DeleteSectionReq) => {
    const response = await axiosInstance.delete(`/section/${sectionId}`);
    return response.data;
};

export const useDeleteSectionAdmin = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteSectionAdmin,
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
        onError: (error) => {
            const err = error as AxiosError<{ message?: string }>;
            showToast.error(`${err.response?.data.message ?? ''}`);
        },
    });
};
