import { useQuery } from '@tanstack/react-query';

import { TRequestMethod, TRequestServiceType, TRequestStatus } from '@/typings/Requests';

import { axiosInstance } from '../axiosInstance';

type GetApprovalDetailsReq = string | undefined;
type GetApprovalDetailsRes =
    | TVillageHeadApprovalDetails
    | TFarmerApprovalDetails
    | TSectionApprovalDetails
    | TTreeTransactionApprovalDetails;

type TApprovalBase = {
    id: number | null;
    rejectedReason: string | null;
    serviceType: TRequestServiceType;
    status: TRequestStatus;
    requesterName: string;
    method: TRequestMethod;
    createdAt: Date;
};

export type TFarmerApprovalDetails = TApprovalBase & {
    identificationPhotoUrl: string | null;
    name: string;
    villageHeadId: number;
    areaId: number;
    areaName: string;
    method: TRequestMethod;
    rejectedReason: string | null;
    requesterName: string;
    sectionId: number;
    sectionName: string;
};

export type TVillageHeadApprovalDetails = TApprovalBase & {
    accountInfo: string;
    bankName: string;
    bankbookPhotoUrl: string | null;
    contractFileUrl: string | null;
    identificationPhotoUrl: string | null;
    userId: string;
    username: string;
    areaId: number;
    areaName: string;
    sectionId: number;
    sectionName: string;
};

export type TSectionApprovalDetails = TApprovalBase & {
    areaId: number;
    latitude: number;
    longitude: number;
    sectionName: string;
    areaName: string;
};

export type TPurchaseApprovalDetails = TApprovalBase & {
    deduction: number;
    paymentAmount: number;
    purchaseDate: Date;
    quantity: number;
    totalPrice: number;
    unitPrice: number;
    remarks: string;
};

export type TTreeTransactionApprovalDetails = TApprovalBase & {
    farmerId: number;
    farmerName: string;
    sectionName: string;
    quantity: number;
    receivedDate: string;
    species: string;
    areaName: string;
};

const getApprovalDetails = async <T = GetApprovalDetailsRes>(
    approvalId: GetApprovalDetailsReq
): Promise<T> => {
    const { data } = await axiosInstance.get(`/approval/${approvalId}`);

    return data.response;
};

export const useGetApprovalDetails = <T = GetApprovalDetailsRes>(
    approvalId: GetApprovalDetailsReq
) => {
    return useQuery<T>({
        queryKey: ['approval', approvalId],
        queryFn: async () => await getApprovalDetails(approvalId),
        enabled: !!approvalId,
    });
};
