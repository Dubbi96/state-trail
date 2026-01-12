import { useQuery } from '@tanstack/react-query';

import { TPageable } from '@/typings/Pageable';
import { TRequestServiceType, TRequestStatus } from '@/typings/Requests';

import { axiosInstance } from '../axiosInstance';

type GetApprovalReq = {
    statuses: TRequestStatus[];
    serviceTypes: TRequestServiceType[];
    pageable: TPageable;
};

const getApproval = async (params: GetApprovalReq) => {
    const { statuses, serviceTypes, pageable } = params;

    const flatParams = new URLSearchParams();

    statuses.forEach((status) => flatParams.append('statuses', status));
    serviceTypes.forEach((type) => flatParams.append('serviceTypes', type));

    flatParams.append('page', pageable.page.toString());
    flatParams.append('size', pageable.size.toString());
    flatParams.append('sort', pageable.sort);

    const { data } = await axiosInstance.get('/approval', {
        params: flatParams,
    });

    return data.response;
};

export const useGetApproval = (params: GetApprovalReq) => {
    return useQuery({
        queryKey: ['approval', JSON.stringify(params)],
        queryFn: async () => await getApproval(params),
    });
};
