import { useState } from 'react';

import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';

import {
    Box,
    Button,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    Typography,
} from '@mui/material';
import { Pencil } from '@phosphor-icons/react';
import { GoogleMap, Marker } from '@react-google-maps/api';

import { useGetVillageHeadDetails } from '@/apis/AppUser/useGetVillageHeadDetails';
import PageLayout from '@/components/PageLayout';
import Title from '@/components/Title';
import { containerStyle } from '@/pages/Locations/LocationRegister';
import EditVillageHeadDialog from '@/pages/VillageHeads/VillageHeadList/edit-dialog';
import NarrowLayout from '@/routers/NarrowLayout';
import { palette } from '@/themes';
import { getFileName } from '@/utils/getFileName';

const VillageHeadDetails = () => {
    const { t } = useTranslation();
    const { id } = useParams();
    const { data } = useGetVillageHeadDetails(id);
    const [openEditDialog, setOpenEditDialog] = useState(false);

    const MAX_WIDTH = '140px';

    const TABLE_HEADER_STYLES = {
        backgroundColor: palette.grey[50],
        maxWidth: MAX_WIDTH,
        width: MAX_WIDTH,
    };

    return (
        <NarrowLayout>
            <Stack>
                <Title title={t('면장 상세')}>
                    <Button
                        variant="outlinedBlue"
                        sx={{ width: '86px', wordBreak: 'keep-all' }}
                        onClick={() => {
                            setOpenEditDialog(true);
                        }}
                        startIcon={<Pencil />}
                    >
                        {t('수정')}
                    </Button>
                </Title>
                <PageLayout gap={'50px'}>
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
                                    <TableCell rowSpan={5}>
                                        {data?.identificationPhotoUrl ? (
                                            <Box
                                                sx={{
                                                    width: '120px',
                                                    height: '160px',
                                                    backgroundImage: `url(${data.identificationPhotoUrl})`,
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
                                                <Typography color={palette.grey[400]}>
                                                    No Image
                                                </Typography>
                                            </Stack>
                                        )}
                                    </TableCell>
                                </TableRow>
                                <TableRow
                                    sx={{
                                        '&:hover': {
                                            backgroundColor: 'transparent',
                                        },
                                    }}
                                >
                                    <TableCell sx={TABLE_HEADER_STYLES}>지역명</TableCell>
                                    <TableCell align="left">{data?.areaInfo.areaName}</TableCell>
                                </TableRow>

                                <TableRow
                                    sx={{
                                        '&:hover': {
                                            backgroundColor: 'transparent',
                                        },
                                    }}
                                >
                                    <TableCell sx={TABLE_HEADER_STYLES}>섹션명</TableCell>
                                    <TableCell align="left">
                                        {data?.sectionInfo.sectionName}
                                    </TableCell>
                                </TableRow>
                                <TableRow
                                    sx={{
                                        '&:hover': {
                                            backgroundColor: 'transparent',
                                        },
                                    }}
                                >
                                    <TableCell sx={TABLE_HEADER_STYLES}>이름</TableCell>
                                    <TableCell align="left">{data?.username}</TableCell>
                                </TableRow>
                                <TableRow
                                    sx={{
                                        '&:hover': {
                                            backgroundColor: 'transparent',
                                        },
                                    }}
                                >
                                    <TableCell sx={TABLE_HEADER_STYLES}>아이디</TableCell>
                                    <TableCell align="left">{data?.userId}</TableCell>
                                </TableRow>
                                <TableRow
                                    sx={{
                                        '&:hover': {
                                            backgroundColor: 'transparent',
                                        },
                                    }}
                                >
                                    <TableCell sx={TABLE_HEADER_STYLES}>계좌 정보</TableCell>
                                    <TableCell colSpan={4} align="left">
                                        {data?.bankName} / {data?.accountInfo}
                                    </TableCell>
                                </TableRow>
                                <TableRow
                                    sx={{
                                        '&:hover': {
                                            backgroundColor: 'transparent',
                                        },
                                    }}
                                >
                                    <TableCell sx={TABLE_HEADER_STYLES}>계약서</TableCell>
                                    <TableCell colSpan={4} align="left">
                                        {data?.contractFileUrl ? (
                                            <Typography
                                                sx={{
                                                    'cursor': 'pointer',
                                                    '&:hover': { textDecoration: 'underline' },
                                                }}
                                                onClick={() => {
                                                    if (!data?.contractFileUrl) return;
                                                    window.open(data?.contractFileUrl, '_blank');
                                                }}
                                            >
                                                {getFileName(data?.contractFileUrl)}
                                            </Typography>
                                        ) : (
                                            <Typography>-</Typography>
                                        )}
                                    </TableCell>
                                </TableRow>
                                <TableRow
                                    sx={{
                                        '&:hover': {
                                            backgroundColor: 'transparent',
                                        },
                                    }}
                                >
                                    <TableCell sx={TABLE_HEADER_STYLES}>통장사본</TableCell>
                                    <TableCell colSpan={4} align="left">
                                        {data?.bankbookPhotoUrl ? (
                                            <Typography
                                                sx={{
                                                    'cursor': 'pointer',
                                                    '&:hover': { textDecoration: 'underline' },
                                                }}
                                                onClick={() => {
                                                    if (!data?.bankbookPhotoUrl) return;
                                                    window.open(data?.bankbookPhotoUrl, '_blank');
                                                }}
                                            >
                                                {getFileName(data?.bankbookPhotoUrl)}
                                            </Typography>
                                        ) : (
                                            <Typography>-</Typography>
                                        )}
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        </Table>
                    </TableContainer>

                    <GoogleMap
                        mapContainerStyle={containerStyle}
                        center={{
                            lat: data?.sectionInfo.latitude || 0,
                            lng: data?.sectionInfo.longitude || 0,
                        }}
                        zoom={13}
                    >
                        <Marker
                            position={{
                                lat: data?.sectionInfo.latitude || 0,
                                lng: data?.sectionInfo.longitude || 0,
                            }}
                        />
                    </GoogleMap>
                </PageLayout>
            </Stack>

            {id && (
                <EditVillageHeadDialog
                    open={openEditDialog}
                    onClose={() => {
                        setOpenEditDialog(false);
                    }}
                    id={String(id)}
                />
            )}
        </NarrowLayout>
    );
};

export default VillageHeadDetails;
