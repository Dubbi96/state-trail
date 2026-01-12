import { useMutation } from '@tanstack/react-query';

import { axiosInstance } from '../axiosInstance';

type PatchMyReq = {
    username: string;
    password: string;
    idCardFile: File | null;
};

const patchMy = async (formData: PatchMyReq) => {
    const response = await axiosInstance.patch('/app-user', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};

export const usePatchMy = () => {
    return useMutation({
        mutationFn: patchMy,
    });
};
