import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';

import { Box, Stack, Typography } from '@mui/material';

import {
    TFarmerApprovalDetails,
    useGetApprovalDetails,
} from '@/apis/Approval/useGetApprovalDetails';
import { palette } from '@/themes';

import RequestDetailsLayout from './RequestDetailsLayout';

const FarmerRequestDetails = () => {
    const { t } = useTranslation();
    const { id } = useParams();

    const { data: details } = useGetApprovalDetails<TFarmerApprovalDetails>(id);

    if (!details) return null;
    return (
        <RequestDetailsLayout>
            <Stack sx={{ gap: '20px' }}>
                <Typography variant="h3/bold">{t('내용')}</Typography>
                <Typography variant="title/semibold">{t('농부 정보')}</Typography>
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
                            <Typography>{details.name}</Typography>
                        </Stack>
                    </Stack>
                </Stack>
            </Stack>
        </RequestDetailsLayout>
    );
};

export default FarmerRequestDetails;
