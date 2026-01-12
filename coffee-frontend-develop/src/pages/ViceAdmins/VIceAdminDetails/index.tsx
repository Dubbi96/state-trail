import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';

import { Box, Button, Stack, Typography } from '@mui/material';
import { GoogleMap, Marker } from '@react-google-maps/api';

import { useGetViceAdminDetails } from '@/apis/AppUser/useGetViceAdminDetails';
import PageLayout from '@/components/PageLayout';
import Title from '@/components/Title';
import { containerStyle } from '@/pages/Locations/LocationRegister';
import { palette } from '@/themes';
import NarrowLayout from '@/routers/NarrowLayout';

const ViceAdminDetails = () => {
    const { t } = useTranslation();
    const { id } = useParams();
    const { data: viceAdminDetail } = useGetViceAdminDetails(id);
    const navigate = useNavigate();

    return (
        <NarrowLayout>

            <Stack>
                <Title title={t('부관리자 상세')}>
                    <Button variant="outlinedBlue" onClick={() => navigate(`/vice-admins/edit/${id}`)}>
                        {t('수정')}
                    </Button>
                </Title>
                <PageLayout gap={'27px'}>
                    <Stack direction={'row'} gap={'20px'} sx={{ alignItems: 'center' }}>
                        {viceAdminDetail?.idCardUrl ? (
                            <Box
                                sx={{
                                    width: '120px',
                                    height: '160px',
                                    backgroundImage: `url(${viceAdminDetail?.idCardUrl})`,
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
                                <Typography>{t('이름')}</Typography>
                                <Typography>{viceAdminDetail?.username}</Typography>
                            </Stack>
                            <Stack>
                                <Typography>{t('아이디')}</Typography>
                                <Typography>{viceAdminDetail?.userId}</Typography>
                            </Stack>

                            <Stack>
                                <Typography>{t('관리 지역')}</Typography>
                                <Typography>{viceAdminDetail?.areaInfo.areaName}</Typography>
                            </Stack>
                        </Stack>
                    </Stack>

                    <Stack>
                        <GoogleMap
                            mapContainerStyle={containerStyle}
                            center={{
                                lat: viceAdminDetail?.areaInfo.latitude || 0,
                                lng: viceAdminDetail?.areaInfo.longitude || 0,
                            }}
                            zoom={13}
                        >
                            <Marker
                                position={{
                                    lat: viceAdminDetail?.areaInfo.latitude || 0,
                                    lng: viceAdminDetail?.areaInfo.longitude || 0,
                                }}
                            />
                        </GoogleMap>
                    </Stack>
                </PageLayout>
            </Stack>

        </NarrowLayout>

    );
};

export default ViceAdminDetails;
