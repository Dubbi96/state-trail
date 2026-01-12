import React from 'react';

import { useForm } from 'react-hook-form';

import {
    Button,
    Checkbox,
    Chip,
    MenuItem,
    Pagination,
    Select,
    Stack,
    TableCell,
    TableRow,
    Typography,
} from '@mui/material';
import dayjs, { Dayjs } from 'dayjs';

import AddPhoto from '@/components/AddPhoto';
import CustomDatePicker from '@/components/CustomDatePicker';
import LabelAndInput from '@/components/LabelAndInput';
import SearchTextField from '@/components/SearchTextField';
import Table from '@/components/Table';
import TextArea from '@/components/TextArea';
import Title from '@/components/Title';
import { useDialog } from '@/hooks/useDialog';
import { TChipColor } from '@/typings/Chip';

const TestComponents = () => {
    const methods = useForm<{
        input: string;
        photo: File | null;
        search: string;
        textArea: string;
    }>({
        defaultValues: { input: '', photo: null, search: '', textArea: '' },
    });
    const { openDialog } = useDialog();

    const renderRow = (row: { name: string; id: string }) => {
        return (
            <TableRow>
                <TableCell>{row.name}</TableCell>
                <TableCell>{row.id}</TableCell>
            </TableRow>
        );
    };

    const [select, setSelect] = React.useState<string>('');
    const [value, setValue] = React.useState<Dayjs | null>(dayjs('2022-04-17'));
    console.log(dayjs(value).format('YYYY-MM-DD'));

    return (
        <Stack sx={{ gap: '30px', padding: '50px', flex: 1 }}>
            <Title title="Title">
                <Stack>
                    <Button variant="containedBlue" size="xSmall">
                        containedBlue
                    </Button>
                </Stack>
            </Title>
            {/* Table */}
            <Table
                headData={['header1', 'header2']}
                bodyData={[
                    { name: 'dummy1', id: 'dummy1' },
                    { name: 'dummy2', id: 'dummy2' },
                ]}
                renderRow={renderRow}
            />
            {/* Table - no Data */}
            <Table headData={['header1', 'header2']} bodyData={undefined} renderRow={renderRow} />
            {/* Table - isLoading */}
            <Table
                headData={['header1', 'header2']}
                bodyData={undefined}
                renderRow={renderRow}
                isLoading={true}
            />
            {/* Button */}
            <Stack sx={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                <Button variant="containedBlue">containedBlue</Button>
                <Button variant="containedRed">containedRed</Button>
                <Button variant="containedWhite">containedWhite</Button>
                <Button variant="containedGrey">containedGrey</Button>
            </Stack>
            {/* Dialog */}
            <Stack sx={{ flexDirection: 'row', gap: '20px' }}>
                <Button
                    onClick={() => {
                        openDialog({
                            title: 'title',
                            description: 'description',
                            variant: 'confirm',
                            primaryAction: {
                                name: '확인',
                                onClick: () => {},
                            },
                            secondaryAction: {
                                name: '취소',
                                onClick: () => {},
                            },
                        });
                    }}
                >
                    confirm Dialog
                </Button>
                <Button
                    onClick={() => {
                        openDialog({
                            title: 'title',
                            description: 'description',
                            variant: 'alert',
                            primaryAction: {
                                name: '확인',
                                onClick: () => {},
                            },
                            secondaryAction: {
                                name: '취소',
                                onClick: () => {},
                            },
                        });
                    }}
                >
                    alert Dialog
                </Button>
            </Stack>
            <Stack gap={'10px'}>
                <Typography>Label and input</Typography>
                <LabelAndInput
                    sx={{ width: '100%' }}
                    fieldName="input"
                    register={methods.register}
                    labelValue="label"
                    placeholder="Label"
                />
                <Typography>Select</Typography>
                <Select
                    value={select}
                    onChange={(e) => setSelect(e.target.value)}
                    renderValue={(selected) => {
                        if (selected === '') {
                            return <Typography>Placeholder 텍스트</Typography>; // placeholder로 보여줄 내용
                        }
                        return selected;
                    }}
                >
                    <MenuItem value="1">1</MenuItem>
                    <MenuItem value="2">2</MenuItem>
                    <MenuItem value="3">3</MenuItem>
                </Select>
                <Typography>Search</Typography>
                <SearchTextField
                    fieldName="search"
                    register={methods.register}
                    placeholder="검색어를 입력하세요."
                />
                <Typography>calendar</Typography>
                <CustomDatePicker value={value} onChange={(newValue) => setValue(newValue)} />
                <Typography>Textarea</Typography>
                <TextArea
                    fieldName="textArea"
                    register={methods.register}
                    placeholder={'텍스트를 작성하시오.'}
                />
            </Stack>

            <Stack gap={'10px'}>
                <Typography>Pagination</Typography>
                <Pagination count={3} />
            </Stack>
            <Stack gap={'10px'}>
                <Typography>Check</Typography>
                <Checkbox />
            </Stack>
            <Stack gap={'10px'}>
                <Typography>Chip</Typography>
                {['blue', 'red', 'yellow'].map((item) => (
                    <Chip key={item} label={item} color={item as TChipColor} />
                ))}
            </Stack>
            <Stack gap={'10px'}>
                <Typography>Add Photo</Typography>
                <AddPhoto fieldName="photo" watch={methods.watch} setValue={methods.setValue} />
            </Stack>
        </Stack>
    );
};

export default TestComponents;
