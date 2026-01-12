import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';

import { Box, Stack, Typography } from '@mui/material';

import {
    TVillageHeadApprovalDetails,
    useGetApprovalDetails,
} from '@/apis/Approval/useGetApprovalDetails';
import { palette } from '@/themes';
import { getFileName } from '@/utils/getFileName';

import RequestDetailsLayout from '../FarmerRequestDetails/RequestDetailsLayout';

const VillageHeadRequestDetails = () => {
    const { id } = useParams();
    const { t } = useTranslation();

    const { data: details } = useGetApprovalDetails<TVillageHeadApprovalDetails>(id);

    if (!details) return null;
    return (
        <RequestDetailsLayout>
            <Stack sx={{ gap: '30px' }}>
                <Typography variant="h3/bold">{t('내용')}</Typography>
                <Typography variant="title/semibold">{t('면장 정보')}</Typography>
                <Stack sx={{ flexDirection: 'row', gap: '30px', alignItems: 'center' }}>
                    {details.identificationPhotoUrl ? (
                        <Box
                            sx={{
                                width: '120px',
                                height: '160px',
                                backgroundImage: `url(${details.identificationPhotoUrl})`,
                                backgroundSize: 'cover',
                                backgroundRepeat: 'no-repeat',
                                backgroundPosition: 'center',
                            }}
                        />
                    ) : (
                        <Stack
                            sx={{
                                width: '120px',
                                height: '160px',
                                backgroundColor: palette.grey[50],
                                justifyContent: 'center',
                                alignItems: 'center',
                            }}
                        >
                            <Typography color={palette.grey[400]}>No Image</Typography>
                        </Stack>
                    )}
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
                            <Typography>{t('지역')}</Typography>
                            <Typography>{details.areaName}</Typography>
                        </Stack>
                        <Stack>
                            <Typography>{t('섹션')}</Typography>
                            <Typography>{details.sectionName}</Typography>
                        </Stack>
                        <Stack>
                            <Typography>{t('이름')}</Typography>
                            <Typography>{details.username}</Typography>
                        </Stack>
                        <Stack>
                            <Typography>{t('아이디')}</Typography>
                            <Typography>{details.userId}</Typography>
                        </Stack>
                    </Stack>
                </Stack>
                <Stack
                    sx={{
                        'gap': '20px',
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
                        <Typography>{t('계좌 정보')}</Typography>
                        <Typography>
                            {details.bankName} / {details.accountInfo}
                        </Typography>
                    </Stack>
                    <Stack>
                        <Typography>{t('계약서')}</Typography>
                        {details.contractFileUrl ? (
                            <Typography
                                sx={{
                                    'cursor': 'pointer',
                                    '&:hover': { textDecoration: 'underline' },
                                }}
                                onClick={() => {
                                    if (!details.contractFileUrl) return;
                                    window.open(details.contractFileUrl, '_blank');
                                }}
                            >
                                {getFileName(details.contractFileUrl)}
                            </Typography>
                        ) : (
                            <Typography>-</Typography>
                        )}
                    </Stack>
                    <Stack>
                        <Typography>{t('통장 사본')}</Typography>
                        {details.bankbookPhotoUrl ? (
                            <Typography
                                sx={{
                                    'cursor': 'pointer',
                                    '&:hover': { textDecoration: 'underline' },
                                }}
                                onClick={() => {
                                    if (!details.bankbookPhotoUrl) return;
                                    window.open(details.bankbookPhotoUrl, '_blank');
                                }}
                            >
                                {getFileName(details.bankbookPhotoUrl)}
                            </Typography>
                        ) : (
                            <Typography>-</Typography>
                        )}
                    </Stack>
                </Stack>
            </Stack>
        </RequestDetailsLayout>
    );
};

export default VillageHeadRequestDetails;
