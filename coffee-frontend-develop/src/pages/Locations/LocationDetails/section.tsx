import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';

import { Paper, Table, TableBody, TableCell, TableContainer, TableRow } from '@mui/material';
import { GoogleMap, Marker } from '@react-google-maps/api';

import { useGetSectionDetails } from '@/apis/Section/useGetSectionDetails';
import { Loading } from '@/components/Loading';
import PageLayout from '@/components/PageLayout';
import Title from '@/components/Title';
import NarrowLayout from '@/routers/NarrowLayout';
import { palette } from '@/themes/palette';

export const SectionDetails = () => {
    const { t } = useTranslation();
    const { id } = useParams();

    const { data: sectionDetails, isLoading } = useGetSectionDetails({ sectionId: Number(id) });

    return (
        <NarrowLayout>
            <Title title={t('섹션 상세')} />
            <PageLayout gap="27px">
                {isLoading ? (
                    <Loading />
                ) : sectionDetails ? (
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
                                        <TableCell>{sectionDetails?.areaName}</TableCell>
                                    </TableRow>
                                    <TableRow
                                        sx={{
                                            '&:hover': {
                                                backgroundColor: 'transparent',
                                            },
                                        }}
                                    >
                                        <TableCell sx={{ backgroundColor: palette.grey[50] }}>
                                            섹션명
                                        </TableCell>
                                        <TableCell>{sectionDetails?.sectionName}</TableCell>
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
                                            {sectionDetails?.latitude} / {sectionDetails?.longitude}
                                        </TableCell>
                                    </TableRow>
                                </TableBody>
                            </Table>
                        </TableContainer>

                        <GoogleMap
                            mapContainerStyle={{ width: '100%', height: '400px' }}
                            center={{
                                lat: sectionDetails.latitude,
                                lng: sectionDetails.longitude,
                            }}
                            zoom={13}
                        >
                            <Marker
                                position={{
                                    lat: sectionDetails.latitude,
                                    lng: sectionDetails.longitude,
                                }}
                            />
                        </GoogleMap>
                    </>
                ) : null}
            </PageLayout>
        </NarrowLayout>
    );
};
