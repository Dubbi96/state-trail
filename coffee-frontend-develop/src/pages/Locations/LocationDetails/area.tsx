import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';

import { Paper, Table, TableBody, TableCell, TableContainer, TableRow } from '@mui/material';
import { GoogleMap, Marker } from '@react-google-maps/api';

import { useGetAreaDetails } from '@/apis/Area/useGetAreaDetails';
import { Loading } from '@/components/Loading';
import PageLayout from '@/components/PageLayout';
import Title from '@/components/Title';
import NarrowLayout from '@/routers/NarrowLayout';
import { palette } from '@/themes/palette';

export const AreaDetails = () => {
    const { t } = useTranslation();
    const { id } = useParams();

    const { data: areaDetails, isLoading } = useGetAreaDetails({ areaId: Number(id) });

    return (
        <NarrowLayout>
            <Title title={t('지역 상세')} />
            <PageLayout gap="27px">
                {isLoading ? (
                    <Loading />
                ) : areaDetails ? (
                    <>
                        <TableContainer component={Paper}>
                            <Table>
                                <TableBody>
                                    <TableRow
                                        sx={{
                                            '&:hover': {
                                                backgroundColor: 'transparent',
                                            },
                                        }}
                                    >
                                        <TableCell sx={{ backgroundColor: palette.grey[50] }}>
                                            지역명
                                        </TableCell>
                                        <TableCell>{areaDetails?.areaName}</TableCell>
                                    </TableRow>
                                    <TableRow
                                        sx={{
                                            '&:hover': {
                                                backgroundColor: 'transparent',
                                            },
                                        }}
                                    >
                                        <TableCell sx={{ backgroundColor: palette.grey[50] }}>
                                            위도/경도
                                        </TableCell>
                                        <TableCell>
                                            {areaDetails?.latitude} / {areaDetails?.longitude}
                                        </TableCell>
                                    </TableRow>
                                </TableBody>
                            </Table>
                        </TableContainer>

                        <GoogleMap
                            mapContainerStyle={{ width: '100%', height: '400px' }}
                            center={{
                                lat: areaDetails.latitude,
                                lng: areaDetails.longitude,
                            }}
                            zoom={13}
                        >
                            <Marker
                                position={{
                                    lat: areaDetails.latitude,
                                    lng: areaDetails.longitude,
                                }}
                            />
                        </GoogleMap>
                    </>
                ) : null}
            </PageLayout>
        </NarrowLayout>
    );
};
