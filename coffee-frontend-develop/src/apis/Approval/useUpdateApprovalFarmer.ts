import { useMutation } from '@tanstack/react-query';
import { AxiosResponse } from 'axios';

import { axiosInstance } from '@/apis/axiosInstance';

export type UpdateApprovalFarmerReq = {
    name: string;
    villageHeadId: number | null;
    identificationPhoto?: File;
    farmerId: number | null;
};

const updateApprovalFarmer = async (param: UpdateApprovalFarmerReq): Promise<AxiosResponse> => {
    const queryParams: Record<string, string> = {
        name: param.name,
        villageHeadId: String(param.villageHeadId),
        approverId: '1',
    };
    const query = new URLSearchParams(queryParams).toString();

    const formData = new FormData();
    if (param.identificationPhoto) {
        formData.append('identificationPhoto', param.identificationPhoto);
    }

    return await axiosInstance.patch(`/approval/farmer/${param.farmerId}?${query}`, formData);
};

export const useUpdateApprovalFarmer = () => {
    return useMutation({
        mutationFn: (param: UpdateApprovalFarmerReq) => updateApprovalFarmer(param),
    });
};
