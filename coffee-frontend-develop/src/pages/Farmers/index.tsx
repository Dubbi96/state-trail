import { MouseEvent, useState } from 'react';

import { useTranslation } from 'react-i18next';

import { MoreHoriz } from '@mui/icons-material';
import {
    Button,
    Divider,
    IconButton,
    Menu,
    MenuItem,
    Stack,
    TableCell,
    TableRow,
} from '@mui/material';
import { Plus } from '@phosphor-icons/react';

import { getCookies } from '@/apis/AppUser/cookie';
import { useDeleteFarmer } from '@/apis/Approval/useDeleteFarmer';
import { GetFarmerListRes, useGetFarmerList } from '@/apis/Farmer/useGetFarmerList';
import PageLayout from '@/components/PageLayout';
import Table from '@/components/Table';
import Title from '@/components/Title';
import { useDialog } from '@/hooks/useDialog';

import { FarmerCreateDialog } from './create-dialog';
import { FarmerDetailsDialog } from './details-dialog';
import { FarmerEditDialog } from './edit-dialog';

const FarmerList = () => {
    const { openDialog } = useDialog();
    const { t } = useTranslation();
    const { data: farmerList } = useGetFarmerList();
    const { mutate: deleteFarmer } = useDeleteFarmer();
    const [openCreateDialog, setOpenCreateDialog] = useState(false);
    const role = getCookies('role');
    const [openEditDialog, setOpenEditDialog] = useState(false);
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [openDetailsDialog, setOpenDetailsDialog] = useState(false);

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
            title: t('해당 농부를 삭제하시겠습니까?'),
            description: t('해당 농부에 속한 나무 수령 목록도 삭제 됩니다.'),
            variant: 'alert',
            primaryAction: {
                name: t('확인'),
                onClick: () => {
                    deleteFarmer({ farmerId: id });
                    handleCloseMenu();
                },
            },
            secondaryAction: { name: t('취소'), onClick: () => {} },
        });
    };

    const renderRow = (row: GetFarmerListRes) => {
        return (
            <TableRow
                key={row.id}
                onClick={() => {
                    setOpenDetailsDialog(true);
                    setSelectedId(row.id);
                }}
            >
                <TableCell>{row.id.toString()}</TableCell>
                <TableCell>{row.farmerName}</TableCell>
                <TableCell>{row.villageHeadName}</TableCell>
                <TableCell>{row.sectionName}</TableCell>
                {role !== 'VILLAGE_HEAD' && (
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
                                    setSelectedId(row.id);
                                    handleCloseMenu();
                                }}
                            >
                                수정하기
                            </MenuItem>
                            <Divider />
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
    };

    return (
        <Stack sx={{ flex: 1, height: '100%' }}>
            <Title title={t('농부 목록')}>
                <Button
                    variant="outlinedBlue"
                    onClick={() => {
                        setOpenCreateDialog(true);
                    }}
                    startIcon={<Plus />}
                >
                    {t('등록')}
                </Button>
            </Title>
            <PageLayout sx={{ flex: 1, minHeight: 0 }}>
                <Table
                    headData={
                        role !== 'VILLAGE_HEAD'
                            ? [t('ID'), t('이름'), t('면장'), t('섹션'), '']
                            : [t('ID'), t('이름'), t('면장'), t('섹션')]
                    }
                    bodyData={farmerList || undefined}
                    renderRow={renderRow}
                />
            </PageLayout>

            <FarmerCreateDialog
                open={openCreateDialog}
                onClose={() => setOpenCreateDialog(false)}
            />

            {selectedId && (
                <FarmerEditDialog
                    open={openEditDialog}
                    onClose={() => {
                        setOpenEditDialog(false);
                        setSelectedId(null);
                    }}
                    farmerId={selectedId}
                />
            )}

            {selectedId && (
                <FarmerDetailsDialog
                    open={openDetailsDialog}
                    onClose={() => {
                        setOpenDetailsDialog(false);
                        setSelectedId(null);
                    }}
                    farmerId={selectedId}
                />
            )}
        </Stack>
    );
};

export default FarmerList;
