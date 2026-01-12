import { useMutation } from '@tanstack/react-query';

import { axiosInstance } from '@/apis/axiosInstance';

export type CreateAreaReq = {
    areaName: string;
    latitude: number;
    longitude: number;
};
const createArea = async (param: CreateAreaReq) => {
    return await axiosInstance.post('/area', param);
};

export const useCreateArea = () => {
    return useMutation({
        mutationFn: (param: CreateAreaReq) => createArea(param),
    });
};
