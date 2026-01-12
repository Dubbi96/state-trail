import { useMutation } from '@tanstack/react-query';

import { TLoginForm } from '@/pages/Accounts/Login';

import { axiosInstance } from '../axiosInstance';

type SignInReq = TLoginForm;

const signIn = async (formData: SignInReq) => {
    const response = await axiosInstance.post('/app-user/sign-in', formData);
    return response.data;
};

export const useSignIn = () => {
    return useMutation({
        mutationFn: signIn,
        onError: (error) => {
            console.log('useMutation onError 발생:', error);
        },
    });
};
