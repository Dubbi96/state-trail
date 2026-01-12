import { useMutation } from '@tanstack/react-query';

import { TRole } from '@/typings/User';

import { axiosInstance } from '../axiosInstance';

type SignUpReq = {
    userId: string;
    username: string;
    password: string;
    role: '' | TRole;
    areaId: number;
    sectionId?: number;
    idCardFile?: File;
};

const signUp = async (formData: SignUpReq) => {
    const response = await axiosInstance.post('/app-user/sign-up', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
};

export const useSignUp = () => {
    return useMutation({
        mutationFn: signUp,
    });
};
