import { MouseEvent, useState } from 'react';

import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';

import { MoreHoriz } from '@mui/icons-material';
import { IconButton, Menu, MenuItem, Stack, TableCell, TableRow } from '@mui/material';

import { GetViceAdminListRes, useGetViceAdminList } from '@/apis/AppUser/useGetViceAdminList';
import PageLayout from '@/components/PageLayout';
import Table from '@/components/Table';
import Title from '@/components/Title';

import { EditViceAdminDialog } from './edit-dialog';

const ViceAdminList = () => {
    const { t } = useTranslation();
    const { data: viceAdminList, isLoading } = useGetViceAdminList();
    const navigate = useNavigate();
    const [openEditDialog, setOpenEditDialog] = useState(false);
    const [viceAdminId, setViceAdminId] = useState<number | null>(null);

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

    // const handleClickDelete = (id: number) => {
    //     openDialog({
    //         title: t('해당 구매 내역을 삭제하시겠습니까?'),
    //         description: t('삭제하면 복구가 불가능합니다.'),
    //         variant: 'alert',
    //         primaryAction: {
    //             name: t('확인'),
    //             onClick: () => {},
    //         },
    //         secondaryAction: { name: t('취소'), onClick: () => {} },
    //     });
    // };

    const renderRow = (row: GetViceAdminListRes) => {
        return (
            <TableRow key={row.id} onClick={() => navigate(`/vice-admins/${row.id}`)}>
                <TableCell>{row.id.toString()}</TableCell>
                <TableCell>{row.userName}</TableCell>
                <TableCell>{row.userId}</TableCell>
                <TableCell>{row.areaInfo.areaName}</TableCell>
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
                                setOpenEditDialog(true);
                                setViceAdminId(row.id);
                                handleCloseMenu();
                            }}
                        >
                            수정하기
                        </MenuItem>
                    </Menu>
                </TableCell>
            </TableRow>
        );
    };

    return (
        <Stack>
            <Title title={t('부관리자 목록')} />
            <PageLayout>
                <Table
                    headData={[t('ID'), t('이름'), t('아이디'), t('관리 지역'), t('')]}
                    bodyData={viceAdminList}
                    renderRow={renderRow}
                    isLoading={isLoading}
                />
            </PageLayout>
            {viceAdminId && (
                <EditViceAdminDialog
                    open={openEditDialog}
                    onClose={() => setOpenEditDialog(false)}
                    viceAdminId={viceAdminId}
                />
            )}
        </Stack>
    );
};
export default ViceAdminList;
