import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';

import { Stack, Typography } from '@mui/material';
import dayjs from 'dayjs';

import {
    TPurchaseApprovalDetails,
    useGetApprovalDetails,
} from '@/apis/Approval/useGetApprovalDetails';

import RequestDetailsLayout from '../FarmerRequestDetails/RequestDetailsLayout';

const TreesPurchaseRequestDetails = () => {
    const { t } = useTranslation();
    const { id } = useParams();
    const { data: details } = useGetApprovalDetails<TPurchaseApprovalDetails>(id);

    if (!details) return null;
    return (
        <RequestDetailsLayout>
            <Stack sx={{ gap: '30px' }}>
                <Typography variant="h3/bold">{t('내용')}</Typography>
                <Typography variant="title/semibold">{t('수매 내역')}</Typography>
                <Stack
                    sx={{
                        'gap': '15px',
                        '& .MuiStack-root': {
                            'flexDirection': 'row',
                            'alignItems': 'center',
                            'gap': '30px',
                            '& .MuiTypography-root': {
                                minWidth: '120px',
                            },
                        },
                    }}
                >
                    <Stack>
                        <Typography>{t('수매 일자')}</Typography>
                        <Typography>{dayjs(details.purchaseDate).format('YYYY-MM-DD')}</Typography>
                    </Stack>
                    <Stack>
                        <Typography>{t('수량')}</Typography>
                        <Typography>{details.quantity.toLocaleString('ko-KR')}</Typography>
                    </Stack>
                    <Stack>
                        <Typography>{t('단가')}</Typography>
                        <Typography>{details.unitPrice.toLocaleString('ko-KR')}</Typography>
                    </Stack>
                    <Stack>
                        <Typography>{t('총액')}</Typography>
                        <Typography>{details.totalPrice.toLocaleString('ko-KR')}</Typography>
                    </Stack>
                    <Stack>
                        <Typography>{t('차감액')}</Typography>
                        <Typography>{details.deduction.toLocaleString('ko-KR')}</Typography>
                    </Stack>
                    <Stack>
                        <Typography>{t('지급액')}</Typography>
                        <Typography>{details.paymentAmount.toLocaleString('ko-KR')}</Typography>
                    </Stack>
                    <Stack>
                        <Typography>{t('비고')}</Typography>
                        <Typography>{details.remarks}</Typography>
                    </Stack>
                </Stack>
            </Stack>
        </RequestDetailsLayout>
    );
};

export default TreesPurchaseRequestDetails;
