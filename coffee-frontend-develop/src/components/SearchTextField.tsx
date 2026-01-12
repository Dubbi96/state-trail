import { FieldValues, Path, RegisterOptions, UseFormRegister } from 'react-hook-form';

import { InputAdornment, TextField, TextFieldProps } from '@mui/material';
import { MagnifyingGlass, X } from '@phosphor-icons/react';

import { palette } from '@/themes';

type SearchTextFieldProps<T extends FieldValues> = Omit<TextFieldProps, 'name'> & {
    disabled?: boolean;
    register: UseFormRegister<T>;
    rules?: RegisterOptions<T, Path<T>>;
    fieldName: Path<T>;
    placeholder?: string;
};
const SearchTextField = <T extends FieldValues>({
    fieldName,
    register,
    rules,
    placeholder,
    ...props
}: SearchTextFieldProps<T>) => {
    return (
        <TextField
            {...props}
            {...register(fieldName, rules)}
            sx={{ ...props.sx }}
            placeholder={placeholder}
            slotProps={{
                input: {
                    startAdornment: (
                        <InputAdornment position="start">
                            <MagnifyingGlass color={palette.grey[500]} />
                        </InputAdornment>
                    ),
                    endAdornment: (
                        <InputAdornment position="end">
                            <X color={palette.grey[500]} />
                        </InputAdornment>
                    ),
                },
            }}
        />
    );
};

export default SearchTextField;
