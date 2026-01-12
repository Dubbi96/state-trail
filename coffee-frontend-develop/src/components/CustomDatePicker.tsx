import React from 'react';

import { InputAdornment, TextField } from '@mui/material';
import { DatePicker, DatePickerProps } from '@mui/x-date-pickers';
import { CalendarBlank } from '@phosphor-icons/react';
import { Dayjs } from 'dayjs';

const CustomDatePicker: React.FC<DatePickerProps<Dayjs>> = ({ ...props }) => {
    const [calendarOpen, setCalendarOpen] = React.useState<boolean>(false);
    return (
        <DatePicker
            open={calendarOpen}
            onClose={() => setCalendarOpen(false)}
            onOpen={() => setCalendarOpen(true)}
            format="YYYY-MM-DD"
            slotProps={{
                popper: {
                    disablePortal: true,
                    modifiers: [
                        {
                            name: 'offset',
                            options: {
                                offset: [0, 8],
                            },
                        },
                    ],
                },
            }}
            slots={{
                textField: (params) => (
                    <TextField
                        {...params}
                        onClick={() => {
                            if (!calendarOpen) setCalendarOpen(true);
                        }}
                        slotProps={{
                            input: {
                                ...params.InputProps,
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <CalendarBlank />
                                    </InputAdornment>
                                ),
                            },
                        }}
                    />
                ),
            }}
            {...props}
        />
    );
};

export default CustomDatePicker;
