import { FormProvider, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';

import { Stack } from '@mui/material';

import { useGetApproval } from '@/apis/Approval/useGetApproval';
import Title from '@/components/Title';
import { TPageable } from '@/typings/Pageable';
import { TRequestServiceType, TRequestStatus } from '@/typings/Requests';

import RequestListFilter from './RequestListFilter';
import RequestListTable from './RequestListTable';

type TRequestListFilterForm = {
    statuses: Record<TRequestStatus, boolean>;
    serviceTypes: Record<TRequestServiceType, boolean>;
    pageable: TPageable;
};

const RequestList = () => {
    const { t } = useTranslation();
    const methods = useForm<TRequestListFilterForm>({
        defaultValues: {
            statuses: {
                PENDING: true,
                APPROVED: true,
                REJECTED: true,
            },
            serviceTypes: {
                VILLAGE_HEAD: true,
                FARMER: true,
                PURCHASE: true,
                SECTION: true,
            },
            pageable: {
                page: 0,
                size: 10,
                sort: 'createdAt,desc',
            },
        },
    });

    const watchedValues = methods.watch();

    const { data, isLoading } = useGetApproval({
        statuses: Object.entries(watchedValues.statuses)
            .map(([key, val]) => (val ? (key as TRequestStatus) : null))
            .filter((v) => v !== null),
        serviceTypes: Object.entries(watchedValues.serviceTypes)
            .map(([key, val]) => (val ? (key as TRequestServiceType) : null))
            .filter((v) => v !== null),
        pageable: watchedValues.pageable,
    });

    return (
        <FormProvider {...methods}>
            <Stack sx={{ width: '100%', maxHeight: '100%', flex: '1' }} component="form">
                <Title title={t('요청 목록')} />
                <Stack
                    sx={{
                        gap: '20px',
                        padding: '0 32px',
                        flex: '1',
                        overflow: 'hidden',
                    }}
                >
                    <RequestListFilter />
                    <RequestListTable data={data} isLoading={isLoading} />
                </Stack>
            </Stack>
        </FormProvider>
    );
};

export default RequestList;
