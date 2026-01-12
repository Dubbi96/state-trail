import { MouseEvent, useState } from 'react';

import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';

import { MoreHoriz } from '@mui/icons-material';
import {
    CircularProgress,
    IconButton,
    Menu,
    MenuItem,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
} from '@mui/material';

import noData from '@assets/noData.svg';

import { getCookies } from '@/apis/AppUser/cookie';
import { useDeleteSection } from '@/apis/Approval/useDeleteSection';
import { useGetAreaWithSectionList } from '@/apis/Area/useGetAreaWithSection';
import { useDeleteSectionAdmin } from '@/apis/Section/useDeleteSectionAdmin';
import PageLayout from '@/components/PageLayout';
import Title from '@/components/Title';
import { useDialog } from '@/hooks/useDialog';
import { TAreaWithSections } from '@/typings/Area';

const LocationList = () => {
    const { t } = useTranslation();
    const { openDialog } = useDialog();
    const navigate = useNavigate();
    const { data: areaWithSectionList, isLoading: areaWithSectionListLoading } =
        useGetAreaWithSectionList();
    const { mutate: deleteSection } = useDeleteSection();
    const { mutate: deleteSectionAdmin } = useDeleteSectionAdmin();
    const headData = [t('ID'), t('지역'), t('섹션'), ''];
    const role = getCookies('role');

    const [menuState, setMenuState] = useState<{
        anchorEl: HTMLElement | null;
        rowId: number | null;
    }>({ anchorEl: null, rowId: null });

    const open = Boolean(menuState.anchorEl);

    const handleClickMenu = (rowId: number) => (e: MouseEvent<HTMLButtonElement>) => {
        e.stopPropagation();
        setMenuState({ anchorEl: e.currentTarget, rowId });
    };

    const handleCloseMenu = () => setMenuState({ anchorEl: null, rowId: null });

    const handleClickDelete = (id: number) => {
        openDialog({
            title: t('해당 섹션을 삭제하시겠습니까?'),
            description: t('섹션을 삭제하면 복구가 불가능합니다.'),
            variant: 'alert',
            primaryAction: {
                name: t('확인'),
                onClick: () => {
                    const submit = role === 'ADMIN' ? deleteSectionAdmin : deleteSection;
                    submit({ sectionId: id });
                },
            },
            secondaryAction: { name: t('취소'), onClick: () => {} },
        });
    };

    const renderRow = (row: TAreaWithSections) => {
        const parentRow = (
            <TableRow key={`area-${row.id}`} onClick={() => navigate(`/locations/area/${row.id}`)}>
                <TableCell>{row.id.toString()}</TableCell>
                <TableCell>{row.areaName}</TableCell>
                <TableCell>-</TableCell>
                {role === 'ADMIN' && (
                    <TableCell onClick={(e) => e.stopPropagation()} align="right">
                        <IconButton
                            id={`basic-button-${row.id}`}
                            size="small"
                            aria-controls={open ? 'basic-menu' : undefined}
                            aria-haspopup="true"
                            aria-expanded={open ? 'true' : undefined}
                            onClick={handleClickMenu(row.id)}
                        >
                            <MoreHoriz />
                        </IconButton>
                        <Menu
                            anchorEl={menuState.anchorEl}
                            open={open && menuState.rowId === row.id}
                            onClose={handleCloseMenu}
                            slotProps={{
                                list: {
                                    'aria-labelledby': 'basic-button',
                                },
                            }}
                        >
                            <MenuItem
                                onClick={() => {
                                    handleClickDelete(row.id);
                                    handleCloseMenu();
                                }}
                            >
                                삭제하기
                            </MenuItem>
                        </Menu>
                    </TableCell>
                )}
            </TableRow>
        );

        const childRows = row.sections.map((item) => (
            <TableRow
                key={`section-${item.id}`}
                onClick={() => navigate(`/locations/section/${item.id}`)}
            >
                <TableCell>{item.id.toString()}</TableCell>
                <TableCell>{row.areaName}</TableCell>
                <TableCell>{item.sectionName}</TableCell>
                {role === 'ADMIN' && (
                    <TableCell onClick={(e) => e.stopPropagation()} align="right">
                        <IconButton
                            id={`basic-button-${row.id}`}
                            size="small"
                            aria-controls={open ? 'basic-menu' : undefined}
                            aria-haspopup="true"
                            aria-expanded={open ? 'true' : undefined}
                            onClick={handleClickMenu(row.id)}
                        >
                            <MoreHoriz />
                        </IconButton>
                        <Menu
                            anchorEl={menuState.anchorEl}
                            open={open && menuState.rowId === row.id}
                            onClose={handleCloseMenu}
                            slotProps={{
                                list: {
                                    'aria-labelledby': 'basic-button',
                                },
                            }}
                        >
                            <MenuItem
                                onClick={() => {
                                    const submit =
                                        role === 'ADMIN' ? deleteSectionAdmin : deleteSection;
                                    submit({ sectionId: item.id });
                                    handleCloseMenu();
                                }}
                            >
                                삭제하기
                            </MenuItem>
                        </Menu>
                    </TableCell>
                )}
            </TableRow>
        ));

        return [parentRow, ...childRows];
    };

    return (
        <Stack>
            <Title title={t('지역 및 섹션 목록')} />
            <PageLayout>
                <TableContainer sx={{ width: '100%', height: '100%' }}>
                    <Table sx={{ height: '100%' }} stickyHeader>
                        <TableHead>
                            <TableRow>
                                {headData.map((item) => {
                                    return <TableCell key={item}>{item}</TableCell>;
                                })}
                            </TableRow>
                        </TableHead>
                        {areaWithSectionListLoading ? (
                            <TableBody>
                                <TableRow>
                                    <TableCell colSpan={headData.length}>
                                        <Stack
                                            sx={{
                                                width: '100%',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                            }}
                                        >
                                            <CircularProgress color="primary" size={30} />
                                        </Stack>
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        ) : areaWithSectionList && areaWithSectionList.length > 0 ? (
                            <TableBody>
                                {areaWithSectionList.map((item) => renderRow(item))}
                            </TableBody>
                        ) : (
                            <TableBody>
                                <TableRow>
                                    <TableCell colSpan={headData.length}>
                                        <Stack
                                            sx={{
                                                width: '100%',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                            }}
                                        >
                                            <img src={noData} alt="No Data" width={60} />
                                        </Stack>
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        )}
                    </Table>
                </TableContainer>
            </PageLayout>
        </Stack>
    );
};

export default LocationList;
