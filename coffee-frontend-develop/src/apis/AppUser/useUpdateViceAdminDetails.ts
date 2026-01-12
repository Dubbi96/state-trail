import { useMutation } from '@tanstack/react-query';
import { AxiosResponse } from 'axios';

import { axiosInstance } from '@/apis/axiosInstance';

type UpdateViceAdminDetailsReq = {
    viceAdminId: number;
    username: string;
    userId: string;
    areaId: number;
    idCardFile: File | null;
};

const updateViceAdminDetails = async (param: UpdateViceAdminDetailsReq): Promise<AxiosResponse> => {
    const { viceAdminId, username, userId, areaId, idCardFile } = param;

    const formData = new FormData();
    formData.append('username', username);
    formData.append('userId', userId);
    formData.append('areaId', areaId.toString());
    if (idCardFile) {
        formData.append('idCardFile', idCardFile);
    }

    return axiosInstance.patch(`/app-user/vice-admin?viceAdminId=${viceAdminId}`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

export const useUpdateViceAdminDetails = () => {
    return useMutation({
        mutationFn: (param: UpdateViceAdminDetailsReq) => updateViceAdminDetails(param),
    });
};
