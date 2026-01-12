import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';

import { Stack, Typography } from '@mui/material';
import { GoogleMap, Marker } from '@react-google-maps/api';

import {
    TSectionApprovalDetails,
    useGetApprovalDetails,
} from '@/apis/Approval/useGetApprovalDetails';
import { containerStyle } from '@/pages/Locations/LocationRegister';

import RequestDetailsLayout from '../FarmerRequestDetails/RequestDetailsLayout';

const SectionRequestDetails = () => {
    const { t } = useTranslation();
    const { id } = useParams();
    const { data: details } = useGetApprovalDetails<TSectionApprovalDetails>(id);

    console.log(details, 'details');

    if (!details) return null;
    return (
        <RequestDetailsLayout>
            <Stack sx={{ gap: '30px' }}>
                <Typography variant="h3/bold">{t('내용')}</Typography>
                <Typography variant="title/semibold">{t('지역 및 섹션 정보')}</Typography>
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
                        <Typography>{t('섹션')}</Typography>
                        <Typography>{details.sectionName}</Typography>
                    </Stack>
                </Stack>

                <GoogleMap
                    mapContainerStyle={containerStyle}
                    center={{ lat: details.latitude, lng: details.longitude }}
                    zoom={13}
                    options={{
                        gestureHandling: 'none',
                    }}
                >
                    <Marker position={{ lat: details.latitude, lng: details.longitude }} />
                </GoogleMap>
            </Stack>
        </RequestDetailsLayout>
    );
};

export default SectionRequestDetails;
