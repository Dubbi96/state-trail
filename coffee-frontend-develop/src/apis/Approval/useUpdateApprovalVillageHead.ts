import { useMutation } from '@tanstack/react-query';
import { AxiosResponse } from 'axios';

import { axiosInstance } from '@/apis/axiosInstance';

type UpdateApprovalVillageHeadReq = {
    appUserId: string;
    userId: string;
    username: string;
    password: string;
    bankName?: string;
    accountInfo?: string;
    sectionId: number;
    approverId: string;
    identificationPhoto: File;
    contractFile: File;
    bankbookPhoto: File;
};

const updateApprovalVillageHead = async (
    param: UpdateApprovalVillageHeadReq
): Promise<AxiosResponse> => {
    const queryParams: Record<string, string> = {
        appUserId: param.appUserId,
        userId: param.userId,
        username: param.username,
        password: param.password,
        sectionId: String(param.sectionId),
        approverId: '1',
    };

    if (param.bankName) queryParams.bankName = param.bankName;
    if (param.accountInfo) queryParams.accountInfo = param.accountInfo;

    const query = new URLSearchParams(queryParams).toString();

    const formData = new FormData();
    formData.append('identificationPhoto', param.identificationPhoto);
    formData.append('contractFile', param.contractFile);
    formData.append('bankbookPhoto', param.bankbookPhoto);

    return await axiosInstance.patch(`/approval/village-head?${query}`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

export const useUpdateApprovalVillageHead = () => {
    return useMutation({
        mutationFn: (param: UpdateApprovalVillageHeadReq) => updateApprovalVillageHead(param),
    });
};
